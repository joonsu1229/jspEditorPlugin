package com.github.codex.jsplite.navigation;

import com.github.codex.jsplite.JspLiteFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JspLiteReferenceContributor extends PsiReferenceContributor {
    private static final int CONTEXT_LIMIT = 180;
    private static final long SEARCH_TIMEOUT_NANOS = 5_000_000_000L;
    private static final Pattern JS_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern FUNCTION_DECLARATION = Pattern.compile(
            "(?:\\bfunction\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\()"
                    + "|(?:\\b(?:var|let|const)\\s+)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\("
                    + "|(?:\\b(?:var|let|const)\\s+)([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:async\\s*)?\\([^;{}]*\\)\\s*=>"
                    + "|(?:\\b(?:var|let|const)\\s+)([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(?:async\\s*)?[A-Za-z_$][A-Za-z0-9_$]*\\s*=>"
                    + "|(?:\\bwindow\\.([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*function\\s*\\()"
    );
    private static final Pattern FUNCTION_CALL = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(");

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns.psiElement(),
                new PsiReferenceProvider() {
                    @Override
                    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                            @NotNull ProcessingContext context) {
                        if (element.getContainingFile().getFileType() != JspLiteFileType.INSTANCE) return PsiReference.EMPTY_ARRAY;
                        IncludeTarget target = IncludeTarget.from(element);
                        if (target != null) return new PsiReference[]{new IncludeReference(element, target.range, target.path)};
                        return FunctionTarget.referencesFrom(element);
                    }
                }
        );
    }

    private static final class IncludeReference extends PsiReferenceBase<PsiElement> {
        private final String path;

        private IncludeReference(@NotNull PsiElement element, @NotNull TextRange range, @NotNull String path) {
            super(element, range, true);
            this.path = path;
        }

        @Override
        public @Nullable PsiElement resolve() {
            return resolveFile(myElement.getProject(), myElement.getContainingFile(), path);
        }

        @Override
        public Object @NotNull [] getVariants() {
            return new Object[0];
        }
    }

    private static @Nullable PsiFile resolveFile(Project project, PsiFile containingFile, String path) {
        if (path.isBlank()) return null;
        VirtualFile current = containingFile.getVirtualFile();
        if (current == null) return null;

        String normalized = path.replace('\\', '/');
        VirtualFile target = null;
        if (normalized.startsWith("/")) {
            target = findFromWebRoot(current, normalized.substring(1));
        } else if (current.getParent() != null) {
            target = current.getParent().findFileByRelativePath(normalized);
        }
        if (target == null) {
            VirtualFile root = project.getBaseDir();
            target = root == null ? null : root.findFileByRelativePath(stripLeadingSlash(normalized));
        }
        return target == null || target.isDirectory() ? null : PsiManager.getInstance(project).findFile(target);
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

    private static String stripLeadingSlash(String path) {
        int start = 0;
        while (start < path.length() && path.charAt(start) == '/') start++;
        return path.substring(start);
    }

    private static final class IncludeTarget {
        private final TextRange range;
        private final String path;

        private IncludeTarget(TextRange range, String path) {
            this.range = range;
            this.path = path;
        }

        private static @Nullable IncludeTarget from(PsiElement element) {
            String token = element.getText();
            if (token.length() < 2) return null;
            char quote = token.charAt(0);
            if ((quote != '"' && quote != '\'') || token.charAt(token.length() - 1) != quote) return null;

            PsiFile file = element.getContainingFile();
            String text = file.getText();
            int tokenStart = element.getTextOffset();
            if (tokenStart < 0 || tokenStart >= text.length()) return null;

            String before = text.substring(Math.max(0, tokenStart - CONTEXT_LIMIT), tokenStart);
            String attribute = previousAttributeName(before);
            if (!"file".equals(attribute) && !"page".equals(attribute)) return null;
            if (!isIncludeContext(before, attribute)) return null;

            String path = token.substring(1, token.length() - 1);
            return new IncludeTarget(TextRange.create(1, token.length() - 1), path);
        }

        private static String previousAttributeName(String before) {
            int eq = before.lastIndexOf('=');
            if (eq < 0) return "";
            int end = eq;
            while (end > 0 && Character.isWhitespace(before.charAt(end - 1))) end--;
            int start = end;
            while (start > 0) {
                char c = before.charAt(start - 1);
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != ':') break;
                start--;
            }
            return before.substring(start, end);
        }

        private static boolean isIncludeContext(String before, String attribute) {
            int directive = before.lastIndexOf("<%@");
            int tag = before.lastIndexOf('<');
            int close = before.lastIndexOf('>');
            if ("file".equals(attribute) && directive >= 0 && directive > close) {
                return before.substring(directive).contains("include");
            }
            if ("page".equals(attribute) && tag >= 0 && tag > close) {
                String open = before.substring(tag);
                return open.startsWith("<jsp:include") || open.startsWith("<jsp:forward");
            }
            return false;
        }
    }

    private static final class FunctionTarget {
        private static PsiReference @NotNull [] referencesFrom(PsiElement element) {
            String token = element.getText();
            if (token.isEmpty()) return PsiReference.EMPTY_ARRAY;
            if (!isQuoted(token) && !JS_NAME.matcher(token).matches() && !token.contains("(")) {
                return PsiReference.EMPTY_ARRAY;
            }

            PsiFile file = element.getContainingFile();
            String fileText = file.getText();
            long deadline = System.nanoTime() + SEARCH_TIMEOUT_NANOS;

            if (isQuoted(token)) {
                String value = token.substring(1, token.length() - 1);
                if (!looksLikeHtmlScriptAttribute(element, value)) return PsiReference.EMPTY_ARRAY;
                Map<String, Integer> declarations = declarations(fileText, deadline);
                if (declarations.isEmpty()) return PsiReference.EMPTY_ARRAY;
                List<PsiReference> references = referencesFromString(element, declarations);
                return references.isEmpty() ? PsiReference.EMPTY_ARRAY : references.toArray(PsiReference[]::new);
            }

            if (JS_NAME.matcher(token).matches() && !isFunctionCallIdentifier(element)) {
                return PsiReference.EMPTY_ARRAY;
            }

            Map<String, Integer> declarations = declarations(fileText, deadline);
            if (declarations.isEmpty()) return PsiReference.EMPTY_ARRAY;

            if (JS_NAME.matcher(token).matches() && declarations.containsKey(token)
                    && isFunctionCallIdentifier(element) && !isFunctionDeclarationIdentifier(element)) {
                return new PsiReference[]{new FunctionReference(element, TextRange.create(0, token.length()), token, declarations.get(token))};
            }

            List<PsiReference> references = referencesFromCodeText(element, declarations);
            return references.isEmpty() ? PsiReference.EMPTY_ARRAY : references.toArray(PsiReference[]::new);
        }

        private static List<PsiReference> referencesFromString(PsiElement element, Map<String, Integer> declarations) {
            List<PsiReference> references = new ArrayList<>();
            String token = element.getText();
            String value = token.substring(1, token.length() - 1);
            if (!isHtmlScriptAttribute(element, value)) return references;

            Matcher matcher = FUNCTION_CALL.matcher(value);
            while (matcher.find()) {
                String name = matcher.group(1);
                Integer targetOffset = declarations.get(name);
                if (targetOffset == null) continue;
                int start = matcher.start(1) + 1;
                int end = matcher.end(1) + 1;
                references.add(new FunctionReference(element, TextRange.create(start, end), name, targetOffset));
            }
            return references;
        }

        private static List<PsiReference> referencesFromCodeText(PsiElement element, Map<String, Integer> declarations) {
            List<PsiReference> references = new ArrayList<>();
            String token = element.getText();
            if (token.length() < 3) return references;

            Matcher matcher = FUNCTION_CALL.matcher(token);
            while (matcher.find()) {
                String name = matcher.group(1);
                Integer targetOffset = declarations.get(name);
                if (targetOffset == null) continue;

                int absoluteStart = element.getTextOffset() + matcher.start(1);
                if (isDeclarationNameAt(element.getContainingFile().getText(), absoluteStart, matcher.end(1) - matcher.start(1))) {
                    continue;
                }
                references.add(new FunctionReference(
                        element,
                        TextRange.create(matcher.start(1), matcher.end(1)),
                        name,
                        targetOffset
                ));
            }
            return references;
        }

        private static boolean isFunctionCallIdentifier(PsiElement element) {
            String text = element.getContainingFile().getText();
            int end = element.getTextOffset() + element.getTextLength();
            return nextNonWhitespace(text, end) == '(';
        }

        private static boolean isFunctionDeclarationIdentifier(PsiElement element) {
            String text = element.getContainingFile().getText();
            int start = element.getTextOffset();
            return isDeclarationNameAt(text, start, element.getTextLength());
        }

        private static boolean isDeclarationNameAt(String text, int start, int length) {
            int end = start + length;
            String previous = previousWord(text, start);
            if ("function".equals(previous)) return true;
            if (nextNonWhitespace(text, end) == '=' && "function".equals(nextWordAfterAssignment(text, end))) return true;
            String before = text.substring(Math.max(0, start - 16), start);
            return before.matches("(?s).*\\b(?:var|let|const)\\s+$")
                    && nextNonWhitespace(text, end) == '='
                    && looksLikeArrowFunctionAfterAssignment(text, end);
        }

        private static boolean isHtmlScriptAttribute(PsiElement element, String value) {
            if (!looksLikeHtmlScriptAttribute(element, value)) return false;
            return true;
        }

        private static boolean looksLikeHtmlScriptAttribute(PsiElement element, String value) {
            String text = element.getContainingFile().getText();
            int tokenStart = element.getTextOffset();
            String before = text.substring(Math.max(0, tokenStart - CONTEXT_LIMIT), tokenStart);
            String attribute = previousAttributeName(before);
            return attribute.startsWith("on") || ("href".equals(attribute) && value.stripLeading().startsWith("javascript:"));
        }

        private static Map<String, Integer> declarations(String text, long deadline) {
            Map<String, Integer> declarations = new LinkedHashMap<>();
            Matcher matcher = FUNCTION_DECLARATION.matcher(text);
            while (matcher.find()) {
                if (timedOut(deadline)) return declarations;
                for (int group = 1; group <= matcher.groupCount(); group++) {
                    String name = matcher.group(group);
                    if (name == null) continue;
                    declarations.putIfAbsent(name, matcher.start(group));
                    break;
                }
            }
            return declarations;
        }

        private static boolean timedOut(long deadline) {
            return System.nanoTime() >= deadline;
        }

        private static boolean isQuoted(String token) {
            if (token.length() < 2) return false;
            char quote = token.charAt(0);
            return (quote == '"' || quote == '\'') && token.charAt(token.length() - 1) == quote;
        }

        private static String previousAttributeName(String before) {
            int eq = before.lastIndexOf('=');
            if (eq < 0) return "";
            int end = eq;
            while (end > 0 && Character.isWhitespace(before.charAt(end - 1))) end--;
            int start = end;
            while (start > 0) {
                char c = before.charAt(start - 1);
                if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != ':') break;
                start--;
            }
            return before.substring(start, end);
        }

        private static char nextNonWhitespace(String text, int offset) {
            for (int i = offset; i < text.length(); i++) {
                char c = text.charAt(i);
                if (!Character.isWhitespace(c)) return c;
            }
            return '\0';
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

        private static String nextWordAfterAssignment(String text, int offset) {
            int i = offset;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
            if (i >= text.length() || text.charAt(i) != '=') return "";
            i++;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
            int start = i;
            while (i < text.length() && isJsNamePart(text.charAt(i))) i++;
            return start < i ? text.substring(start, i) : "";
        }

        private static boolean looksLikeArrowFunctionAfterAssignment(String text, int offset) {
            int eq = text.indexOf('=', offset);
            if (eq < 0) return false;
            int arrow = text.indexOf("=>", eq + 1);
            if (arrow < 0) return false;
            int semicolon = text.indexOf(';', eq + 1);
            int brace = text.indexOf('{', eq + 1);
            int stop = semicolon < 0 ? brace : (brace < 0 ? semicolon : Math.min(semicolon, brace));
            return stop < 0 || arrow < stop;
        }

        private static boolean isJsNamePart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }
    }

    private static final class FunctionReference extends PsiReferenceBase<PsiElement> {
        private final String name;
        private final int targetOffset;

        private FunctionReference(@NotNull PsiElement element, @NotNull TextRange range, @NotNull String name, int targetOffset) {
            super(element, range, true);
            this.name = name;
            this.targetOffset = targetOffset;
        }

        @Override
        public @Nullable PsiElement resolve() {
            PsiFile file = myElement.getContainingFile();
            if (targetOffset < 0 || targetOffset >= file.getTextLength()) return null;
            PsiElement target = file.findElementAt(targetOffset);
            return target == null ? file : target;
        }

        @Override
        public Object @NotNull [] getVariants() {
            return new Object[]{name};
        }
    }
}
