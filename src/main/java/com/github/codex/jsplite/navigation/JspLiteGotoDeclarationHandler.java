package com.github.codex.jsplite.navigation;

import com.github.codex.jsplite.JspLiteFileType;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JspLiteGotoDeclarationHandler implements GotoDeclarationHandler {
    private static final long SEARCH_TIMEOUT_NANOS = 5_000_000_000L;
    private static final Pattern SCRIPT_SRC = Pattern.compile(
            "<script\\b[^>]*\\bsrc\\s*=\\s*(['\"])(.*?)\\1[^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern FUNCTION_DECLARATION = Pattern.compile(
            "(?:\\bfunction\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\()"
                    + "|(?:\\b(?:var|let|const)\\s+)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\("
                    + "|(?:\\b(?:var|let|const)\\s+)([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:async\\s*)?\\([^;{}]*\\)\\s*=>"
                    + "|(?:\\b(?:var|let|const)\\s+)([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:async\\s*)?[A-Za-z_$][A-Za-z0-9_$]*\\s*=>"
                    + "|(?:\\bwindow\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\()"
                    + "|(?:\\b[A-Za-z_$][A-Za-z0-9_$]*\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\()"
                    + "|(?:\\b[A-Za-z_$][A-Za-z0-9_$]*\\.prototype\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\()"
                    + "|(?:\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*:\\s*function\\s*\\()"
                    + "|(?:\\b['\"]([A-Za-z_$][A-Za-z0-9_$]*)['\"]\\s*:\\s*function\\s*\\()"
                    + "|(?:\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*:\\s*(?:async\\s*)?\\([^;{}]*\\)\\s*=>)"
                    + "|(?:\\b['\"]([A-Za-z_$][A-Za-z0-9_$]*)['\"]\\s*:\\s*(?:async\\s*)?\\([^;{}]*\\)\\s*=>)"
                    + "|(?:\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^)]*\\)\\s*\\{)"
    );
    private static final Pattern VARIABLE_DECLARATION = Pattern.compile("\\b(?:var|let|const)\\s+([^;]+)");
    private static final Pattern FUNCTION_PARAMETERS = Pattern.compile(
            "\\bfunction(?:\\s+[A-Za-z_$][A-Za-z0-9_$]*)?\\s*\\(([^)]*)\\)"
    );
    private static final Pattern JSP_VAR_ATTRIBUTE = Pattern.compile(
            "\\bvar\\s*=\\s*(['\"])([A-Za-z_$][A-Za-z0-9_$]*)\\1",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
        if (sourceElement == null) return null;
        PsiFile file = sourceElement.getContainingFile();
        if (file == null || file.getFileType() != JspLiteFileType.INSTANCE) return null;

        String text = file.getText();
        SymbolAtOffset symbol = symbolAt(text, offset);
        if (symbol == null) return null;

        long deadline = System.nanoTime() + SEARCH_TIMEOUT_NANOS;
        int targetOffset = declarationOffset(text, symbol.owner(), symbol.lookupName(), symbol.start(), symbol.includeVariables(), deadline);
        if (targetOffset >= 0) {
            PsiElement target = file.findElementAt(targetOffset);
            return new PsiElement[]{new OffsetNavigationElement(file, targetOffset, symbol.lookupName())};
        }

        PsiElement externalTarget = externalDeclaration(file, symbol.owner(), symbol.lookupName(), deadline);
        return externalTarget == null ? null : new PsiElement[]{externalTarget};
    }

    private static @Nullable SymbolAtOffset symbolAt(String text, int offset) {
        if (text.isEmpty()) return null;
        NameAtOffset name = nameAt(text, offset);
        if (name == null) return null;

        QualifiedName qualified = qualifiedNameAt(text, name);
        if (qualified != null) {
            if (!qualified.isCallableSegment(name)) {
                if (!looksLikeVariableReference(text, name)) return null;
                return new SymbolAtOffset("", name.value(), name.start(), true);
            }
            String lookupName = qualified.lastSegment();
            if (!lookupName.isBlank()) return new SymbolAtOffset(qualified.owner(), lookupName, qualified.start(), false);
        }

        if (looksLikeCall(text, name.end())) {
            return new SymbolAtOffset("", name.value(), name.start(), false);
        }
        if (!looksLikeVariableReference(text, name)) return null;
        return new SymbolAtOffset("", name.value(), name.start(), true);
    }

    private static @Nullable NameAtOffset nameAt(String text, int offset) {
        int cursor = Math.max(0, Math.min(offset, text.length() - 1));
        if (!isJsNamePart(text.charAt(cursor)) && cursor > 0 && isJsNamePart(text.charAt(cursor - 1))) {
            cursor--;
        }
        if (!isJsNamePart(text.charAt(cursor))) return null;

        int start = cursor;
        while (start > 0 && isJsNamePart(text.charAt(start - 1))) start--;
        int end = cursor + 1;
        while (end < text.length() && isJsNamePart(text.charAt(end))) end++;
        return new NameAtOffset(text.substring(start, end), start, end);
    }

    private static @Nullable QualifiedName qualifiedNameAt(String text, NameAtOffset name) {
        int start = name.start();
        int end = name.end();

        while (true) {
            int dot = previousNonWhitespaceOffset(text, start);
            if (dot < 0 || text.charAt(dot) != '.') break;
            int segmentEnd = dot;
            int segmentStart = previousNameStart(text, segmentEnd);
            if (segmentStart < 0) break;
            start = segmentStart;
        }

        while (true) {
            int dot = nextNonWhitespaceOffset(text, end);
            if (dot < 0 || text.charAt(dot) != '.') break;
            int segmentStart = nextNonWhitespaceOffset(text, dot + 1);
            if (segmentStart < 0 || !isJsNamePart(text.charAt(segmentStart))) break;
            int segmentEnd = segmentStart + 1;
            while (segmentEnd < text.length() && isJsNamePart(text.charAt(segmentEnd))) segmentEnd++;
            end = segmentEnd;
        }

        if (start == name.start() && end == name.end()) return null;
        return new QualifiedName(text.substring(start, end), start, end);
    }

    private static boolean looksLikeCall(String text, int offset) {
        return nextNonWhitespace(text, offset) == '(';
    }

    private static boolean looksLikeVariableReference(String text, NameAtOffset name) {
        if (isJavaScriptKeyword(name.value())) return false;
        if (isDeclarationIdentifier(text, name)) return false;
        char next = nextNonWhitespace(text, name.end());
        return next != ':' && next != '{';
    }

    private static boolean isDeclarationIdentifier(String text, NameAtOffset name) {
        String previous = previousWord(text, name.start());
        return "var".equals(previous) || "let".equals(previous) || "const".equals(previous) || "function".equals(previous);
    }

    private static boolean isJavaScriptKeyword(String value) {
        switch (value) {
            case "break":
            case "case":
            case "catch":
            case "class":
            case "const":
            case "continue":
            case "debugger":
            case "default":
            case "delete":
            case "do":
            case "else":
            case "export":
            case "extends":
            case "finally":
            case "for":
            case "function":
            case "if":
            case "import":
            case "in":
            case "instanceof":
            case "let":
            case "new":
            case "return":
            case "super":
            case "switch":
            case "this":
            case "throw":
            case "try":
            case "typeof":
            case "var":
            case "void":
            case "while":
            case "with":
            case "yield":
            case "async":
            case "await":
            case "true":
            case "false":
            case "null":
            case "undefined":
                return true;
            default:
                return false;
        }
    }

    private static int declarationOffset(String text, String owner, String name, int sourceOffset, boolean includeVariables, long deadline) {
        if (timedOut(deadline) || !text.contains(name)) return -1;
        if (!owner.isBlank()) {
            int ownedOffset = ownedDeclarationOffset(text, owner, name, sourceOffset, deadline);
            return ownedOffset;
        }

        Matcher matcher = FUNCTION_DECLARATION.matcher(text);
        while (matcher.find()) {
            if (timedOut(deadline)) return -1;
            for (int group = 1; group <= matcher.groupCount(); group++) {
                String declared = matcher.group(group);
                if (!name.equals(declared)) continue;
                int offset = matcher.start(group);
                if (offset != sourceOffset) return offset;
            }
        }
        if (includeVariables) {
            int variableOffset = variableDeclarationOffset(text, name, sourceOffset, deadline);
            if (variableOffset >= 0) return variableOffset;
            int parameterOffset = parameterDeclarationOffset(text, name, sourceOffset, deadline);
            if (parameterOffset >= 0) return parameterOffset;
            int jspVariableOffset = jspVariableDeclarationOffset(text, name, sourceOffset, deadline);
            if (jspVariableOffset >= 0) return jspVariableOffset;
        }
        return -1;
    }

    private static int variableDeclarationOffset(String text, String name, int sourceOffset, long deadline) {
        int bestBeforeSource = -1;
        int firstAfterSource = -1;
        Matcher matcher = VARIABLE_DECLARATION.matcher(text);
        while (matcher.find()) {
            if (timedOut(deadline)) return -1;
            int offset = variableNameOffsetInDeclaration(text, matcher.start(1), matcher.end(1), name);
            if (offset < 0 || offset == sourceOffset) continue;
            if (sourceOffset < 0) return offset;
            if (offset < sourceOffset) {
                bestBeforeSource = Math.max(bestBeforeSource, offset);
            } else if (firstAfterSource < 0) {
                firstAfterSource = offset;
            }
        }
        return bestBeforeSource >= 0 ? bestBeforeSource : firstAfterSource;
    }

    private static int parameterDeclarationOffset(String text, String name, int sourceOffset, long deadline) {
        int bestBeforeSource = -1;
        Matcher matcher = FUNCTION_PARAMETERS.matcher(text);
        while (matcher.find()) {
            if (timedOut(deadline)) return -1;
            int bodyStart = nextNonWhitespaceOffset(text, matcher.end());
            if (bodyStart < 0 || text.charAt(bodyStart) != '{') continue;
            int bodyEnd = matchingBraceOffset(text, bodyStart, deadline);
            if (sourceOffset >= 0 && bodyEnd >= 0 && (sourceOffset < bodyStart || sourceOffset > bodyEnd)) continue;

            int offset = parameterNameOffset(text, matcher.start(1), matcher.end(1), name);
            if (offset < 0 || offset == sourceOffset) continue;
            if (sourceOffset < 0) return offset;
            if (offset < sourceOffset) bestBeforeSource = Math.max(bestBeforeSource, offset);
        }
        return bestBeforeSource;
    }

    private static int jspVariableDeclarationOffset(String text, String name, int sourceOffset, long deadline) {
        int bestBeforeSource = -1;
        int firstAfterSource = -1;
        Matcher matcher = JSP_VAR_ATTRIBUTE.matcher(text);
        while (matcher.find()) {
            if (timedOut(deadline)) return -1;
            if (!name.equals(matcher.group(2)) || !isInsideTag(text, matcher.start())) continue;
            int offset = matcher.start(2);
            if (offset == sourceOffset) continue;
            if (sourceOffset < 0) return offset;
            if (offset < sourceOffset) {
                bestBeforeSource = Math.max(bestBeforeSource, offset);
            } else if (firstAfterSource < 0) {
                firstAfterSource = offset;
            }
        }
        return bestBeforeSource >= 0 ? bestBeforeSource : firstAfterSource;
    }

    private static boolean isInsideTag(String text, int offset) {
        int open = text.lastIndexOf('<', offset);
        int close = text.lastIndexOf('>', offset);
        return open >= 0 && open > close;
    }

    private static int parameterNameOffset(String text, int start, int end, String name) {
        int cursor = start;
        while (cursor < end) {
            cursor = nextDeclarationNameOffset(text, cursor, end);
            if (cursor < 0) return -1;
            int nameEnd = cursor + 1;
            while (nameEnd < end && isJsNamePart(text.charAt(nameEnd))) nameEnd++;
            if (name.equals(text.substring(cursor, nameEnd))) return cursor;
            cursor = nameEnd;
            while (cursor < end && text.charAt(cursor) != ',') cursor++;
            if (cursor < end) cursor++;
        }
        return -1;
    }

    private static int matchingBraceOffset(String text, int openBrace, long deadline) {
        int depth = 0;
        char quote = '\0';
        for (int i = openBrace; i < text.length(); i++) {
            if ((i & 1023) == 0 && timedOut(deadline)) return -1;
            char c = text.charAt(i);
            if (quote != '\0') {
                if (c == quote && text.charAt(i - 1) != '\\') quote = '\0';
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static int variableNameOffsetInDeclaration(String text, int start, int end, String name) {
        int cursor = start;
        while (cursor < end) {
            cursor = nextDeclarationNameOffset(text, cursor, end);
            if (cursor < 0) return -1;
            int nameEnd = cursor + 1;
            while (nameEnd < end && isJsNamePart(text.charAt(nameEnd))) nameEnd++;
            if (name.equals(text.substring(cursor, nameEnd))) return cursor;
            cursor = skipInitializer(text, nameEnd, end);
            if (cursor < end && text.charAt(cursor) == ',') cursor++;
        }
        return -1;
    }

    private static int nextDeclarationNameOffset(String text, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || c == ',') continue;
            return isJsNameStart(c) ? i : -1;
        }
        return -1;
    }

    private static int skipInitializer(String text, int start, int end) {
        int i = start;
        while (i < end && Character.isWhitespace(text.charAt(i))) i++;
        if (i >= end || text.charAt(i) != '=') return i;
        int paren = 0;
        int bracket = 0;
        int brace = 0;
        char quote = '\0';
        for (i++; i < end; i++) {
            char c = text.charAt(i);
            if (quote != '\0') {
                if (c == quote && text.charAt(i - 1) != '\\') quote = '\0';
                continue;
            }
            if (c == '"' || c == '\'' || c == '`') {
                quote = c;
            } else if (c == '(') {
                paren++;
            } else if (c == ')' && paren > 0) {
                paren--;
            } else if (c == '[') {
                bracket++;
            } else if (c == ']' && bracket > 0) {
                bracket--;
            } else if (c == '{') {
                brace++;
            } else if (c == '}' && brace > 0) {
                brace--;
            } else if (c == ',' && paren == 0 && bracket == 0 && brace == 0) {
                return i;
            }
        }
        return i;
    }

    private static int ownedDeclarationOffset(String text, String owner, String name, int sourceOffset, long deadline) {
        Pattern owned = Pattern.compile("\\b" + Pattern.quote(owner) + "\\s*\\.\\s*" + Pattern.quote(name)
                + "\\s*=\\s*(?:function\\s*\\(|(?:async\\s*)?\\([^;{}]*\\)\\s*=>|(?:async\\s*)?[A-Za-z_$][A-Za-z0-9_$]*\\s*=>)");
        Matcher matcher = owned.matcher(text);
        while (matcher.find()) {
            if (timedOut(deadline)) return -1;
            int nameAt = text.indexOf(name, matcher.start());
            if (nameAt >= 0 && nameAt <= matcher.end() && nameAt != sourceOffset) return nameAt;
        }
        return -1;
    }

    private static @Nullable PsiElement externalDeclaration(PsiFile jspFile, String owner, String name, long deadline) {
        for (VirtualFile script : scriptFiles(jspFile)) {
            if (timedOut(deadline)) return null;
            PsiElement target = declarationInFile(jspFile, script, owner, name, deadline);
            if (target != null) return target;
        }

        for (VirtualFile script : indexedJavaScriptFiles(jspFile.getProject())) {
            if (timedOut(deadline)) return null;
            PsiElement target = declarationInFile(jspFile, script, owner, name, deadline);
            if (target != null) return target;
        }
        return null;
    }

    private static @Nullable PsiElement declarationInFile(PsiFile contextFile, VirtualFile script, String owner, String name, long deadline) {
        if (timedOut(deadline)) return null;
        PsiFile scriptPsi = PsiManager.getInstance(contextFile.getProject()).findFile(script);
        if (scriptPsi == null) return null;
        String text = scriptPsi.getText();
        if (!text.contains(name)) return null;
        if (!owner.isBlank() && !text.contains(owner + "." + name) && !text.contains(owner + " ." + name)) return null;
        int offset = declarationOffset(text, owner, name, -1, true, deadline);
        if (offset < 0) return null;
        return new OffsetNavigationElement(scriptPsi, offset, name);
    }

    private static List<VirtualFile> scriptFiles(PsiFile jspFile) {
        List<VirtualFile> files = new ArrayList<>();
        VirtualFile current = jspFile.getVirtualFile();
        if (current == null) return files;

        Matcher matcher = SCRIPT_SRC.matcher(jspFile.getText());
        while (matcher.find()) {
            String src = normalizeScriptSrc(matcher.group(2));
            if (src.isBlank() || isExternalUrl(src) || !src.endsWith(".js")) continue;
            VirtualFile file = resolveScriptFile(jspFile, current, src);
            if (file != null && !file.isDirectory() && !files.contains(file)) {
                files.add(file);
            }
        }
        return files;
    }

    private static String normalizeScriptSrc(String src) {
        String normalized = src.replace('\\', '/').replaceAll("\\$\\{[^}]+}", "");
        int query = normalized.indexOf('?');
        if (query >= 0) normalized = normalized.substring(0, query);
        int hash = normalized.indexOf('#');
        if (hash >= 0) normalized = normalized.substring(0, hash);
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private static boolean isExternalUrl(String src) {
        String lower = src.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("//");
    }

    private static @Nullable VirtualFile resolveScriptFile(PsiFile jspFile, VirtualFile current, String path) {
        VirtualFile target = null;
        if (current.getParent() != null) {
            target = current.getParent().findFileByRelativePath(path);
        }
        if (target == null) {
            target = findFromWebRoot(current, path);
        }
        if (target == null) {
            VirtualFile root = jspFile.getProject().getBaseDir();
            target = root == null ? null : root.findFileByRelativePath(path);
        }
        if (target == null) {
            target = findIndexedBySuffix(jspFile.getProject(), path);
        }
        return target;
    }

    private static @Nullable VirtualFile findFromWebRoot(VirtualFile current, String relativePath) {
        VirtualFile cursor = current.isDirectory() ? current : current.getParent();
        while (cursor != null) {
            String name = cursor.getName();
            if ("webapp".equals(name) || "WebContent".equals(name) || "web".equals(name)) {
                VirtualFile target = cursor.findFileByRelativePath(relativePath);
                if (target != null) return target;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static @Nullable VirtualFile findIndexedBySuffix(Project project, String suffix) {
        String fileName = suffix.substring(suffix.lastIndexOf('/') + 1);
        for (VirtualFile file : FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project))) {
            String path = file.getPath().replace('\\', '/');
            if (path.endsWith("/" + suffix) || path.endsWith(fileName)) return file;
        }
        return null;
    }

    private static Collection<VirtualFile> indexedJavaScriptFiles(Project project) {
        return FilenameIndex.getAllFilesByExt(project, "js", GlobalSearchScope.projectScope(project));
    }

    private static boolean timedOut(long deadline) {
        return System.nanoTime() >= deadline;
    }

    private static char nextNonWhitespace(String text, int offset) {
        int found = nextNonWhitespaceOffset(text, offset);
        return found < 0 ? '\0' : text.charAt(found);
    }

    private static int nextNonWhitespaceOffset(String text, int offset) {
        for (int i = offset; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) return i;
        }
        return -1;
    }

    private static int previousNonWhitespaceOffset(String text, int offset) {
        for (int i = offset - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) return i;
        }
        return -1;
    }

    private static int previousNameStart(String text, int offset) {
        int end = offset;
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) end--;
        if (end <= 0 || !isJsNamePart(text.charAt(end - 1))) return -1;
        int start = end - 1;
        while (start > 0 && isJsNamePart(text.charAt(start - 1))) start--;
        return start;
    }

    private static String previousWord(String text, int offset) {
        int i = offset - 1;
        while (i >= 0 && Character.isWhitespace(text.charAt(i))) i--;
        while (i >= 0 && !isJsNamePart(text.charAt(i))) i--;
        if (i < 0) return "";
        int end = i + 1;
        while (i >= 0 && isJsNamePart(text.charAt(i))) i--;
        return text.substring(i + 1, end);
    }

    private static boolean isJsNameStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean isJsNamePart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private static final class NameAtOffset {
        private final String value;
        private final int start;
        private final int end;

        private NameAtOffset(String value, int start, int end) {
            this.value = value;
            this.start = start;
            this.end = end;
        }

        private String value() {
            return value;
        }

        private int start() {
            return start;
        }

        private int end() {
            return end;
        }
    }

    private static final class SymbolAtOffset {
        private final String owner;
        private final String lookupName;
        private final int start;
        private final boolean includeVariables;

        private SymbolAtOffset(String owner, String lookupName, int start, boolean includeVariables) {
            this.owner = owner;
            this.lookupName = lookupName;
            this.start = start;
            this.includeVariables = includeVariables;
        }

        private String owner() {
            return owner;
        }

        private String lookupName() {
            return lookupName;
        }

        private int start() {
            return start;
        }

        private boolean includeVariables() {
            return includeVariables;
        }
    }

    private static final class QualifiedName {
        private final String value;
        private final int start;
        private final int end;

        private QualifiedName(String value, int start, int end) {
            this.value = value;
            this.start = start;
            this.end = end;
        }

        private int start() {
            return start;
        }

        private boolean isCallableSegment(NameAtOffset name) {
            int dot = value.lastIndexOf('.');
            int lastSegmentStart = dot < 0 ? start : start + dot + 1;
            return name.start() >= lastSegmentStart;
        }

        private String owner() {
            int dot = value.lastIndexOf('.');
            return dot < 0 ? "" : value.substring(0, dot).strip();
        }

        private String lastSegment() {
            int dot = value.lastIndexOf('.');
            return dot < 0 ? value : value.substring(dot + 1);
        }
    }

    private static final class OffsetNavigationElement extends FakePsiElement {
        private final PsiFile file;
        private final int offset;
        private final String name;

        private OffsetNavigationElement(PsiFile file, int offset, String name) {
            this.file = file;
            this.offset = offset;
            this.name = name;
        }

        @Override
        public PsiElement getParent() {
            return file;
        }

        @Override
        public PsiFile getContainingFile() {
            return file;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void navigate(boolean requestFocus) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null) {
                new OpenFileDescriptor(file.getProject(), virtualFile, offset).navigate(requestFocus);
            }
        }

        @Override
        public boolean canNavigate() {
            return file.getVirtualFile() != null;
        }

        @Override
        public boolean canNavigateToSource() {
            return canNavigate();
        }
    }
}
