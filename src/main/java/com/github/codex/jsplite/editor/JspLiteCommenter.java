package com.github.codex.jsplite.editor;

import com.intellij.codeInsight.generation.CommenterDataHolder;
import com.intellij.codeInsight.generation.SelfManagingCommenter;
import com.intellij.codeInsight.generation.SelfManagingCommenterUtil;
import com.intellij.lang.Commenter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

public final class JspLiteCommenter implements Commenter, SelfManagingCommenter<JspLiteCommenter.CommentingState> {
    private static final String JS_LINE_PREFIX = "//";
    private static final String JS_BLOCK_PREFIX = "/*";
    private static final String JS_BLOCK_SUFFIX = "*/";
    private static final String HTML_BLOCK_PREFIX = "<!--";
    private static final String HTML_BLOCK_SUFFIX = "-->";

    @Override
    public @Nullable String getLineCommentPrefix() {
        return JS_LINE_PREFIX;
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return HTML_BLOCK_PREFIX;
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return HTML_BLOCK_SUFFIX;
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return "<%--";
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return "--%>";
    }

    @Override
    public CommentingState createLineCommentingState(int startLine, int endLine, Document document, PsiFile file) {
        int offset = document.getLineStartOffset(startLine);
        return new CommentingState(isInsideScript(document, offset));
    }

    @Override
    public CommentingState createBlockCommentingState(int selectionStart, int selectionEnd, Document document, PsiFile file) {
        return new CommentingState(isInsideScript(document, selectionStart));
    }

    @Override
    public void commentLine(int line, int offset, Document document, CommentingState state) {
        if (state.script) {
            document.insertString(offset, JS_LINE_PREFIX);
            return;
        }

        int lineEnd = document.getLineEndOffset(line);
        document.insertString(lineEnd, HTML_BLOCK_SUFFIX);
        document.insertString(offset, HTML_BLOCK_PREFIX);
    }

    @Override
    public void uncommentLine(int line, int offset, Document document, CommentingState state) {
        if (state.script) {
            int prefixEnd = offset + JS_LINE_PREFIX.length();
            if (prefixEnd <= document.getTextLength()
                    && matches(document.getCharsSequence(), offset, prefixEnd, JS_LINE_PREFIX)) {
                document.deleteString(offset, prefixEnd);
            }
            return;
        }

        int lineEnd = document.getLineEndOffset(line);
        int prefixEnd = offset + HTML_BLOCK_PREFIX.length();
        int suffixStart = lineEnd - HTML_BLOCK_SUFFIX.length();
        if (prefixEnd <= document.getTextLength()
                && suffixStart >= prefixEnd
                && matches(document.getCharsSequence(), offset, prefixEnd, HTML_BLOCK_PREFIX)
                && matches(document.getCharsSequence(), suffixStart, lineEnd, HTML_BLOCK_SUFFIX)) {
            document.deleteString(suffixStart, lineEnd);
            document.deleteString(offset, prefixEnd);
        }
    }

    @Override
    public boolean isLineCommented(int line, int offset, Document document, CommentingState state) {
        if (state.script) {
            int prefixEnd = offset + JS_LINE_PREFIX.length();
            return prefixEnd <= document.getTextLength()
                    && matches(document.getCharsSequence(), offset, prefixEnd, JS_LINE_PREFIX);
        }

        int lineEnd = document.getLineEndOffset(line);
        int prefixEnd = offset + HTML_BLOCK_PREFIX.length();
        int suffixStart = lineEnd - HTML_BLOCK_SUFFIX.length();
        return prefixEnd <= document.getTextLength()
                && suffixStart >= prefixEnd
                && matches(document.getCharsSequence(), offset, prefixEnd, HTML_BLOCK_PREFIX)
                && matches(document.getCharsSequence(), suffixStart, lineEnd, HTML_BLOCK_SUFFIX);
    }

    @Override
    public String getCommentPrefix(int line, Document document, CommentingState state) {
        return state.script ? JS_LINE_PREFIX : HTML_BLOCK_PREFIX;
    }

    @Override
    public TextRange getBlockCommentRange(int selectionStart, int selectionEnd, Document document, CommentingState state) {
        return SelfManagingCommenterUtil.getBlockCommentRange(
                selectionStart,
                selectionEnd,
                document,
                blockPrefix(state),
                blockSuffix(state)
        );
    }

    @Override
    public String getBlockCommentPrefix(int selectionStart, Document document, CommentingState state) {
        return blockPrefix(state);
    }

    @Override
    public String getBlockCommentSuffix(int selectionEnd, Document document, CommentingState state) {
        return blockSuffix(state);
    }

    @Override
    public void uncommentBlockComment(int startOffset, int endOffset, Document document, CommentingState state) {
        SelfManagingCommenterUtil.uncommentBlockComment(startOffset, endOffset, document, blockPrefix(state), blockSuffix(state));
    }

    @Override
    public TextRange insertBlockComment(int startOffset, int endOffset, Document document, CommentingState state) {
        return SelfManagingCommenterUtil.insertBlockComment(startOffset, endOffset, document, blockPrefix(state), blockSuffix(state));
    }

    private static String blockPrefix(CommentingState state) {
        return state.script ? JS_BLOCK_PREFIX : HTML_BLOCK_PREFIX;
    }

    private static String blockSuffix(CommentingState state) {
        return state.script ? JS_BLOCK_SUFFIX : HTML_BLOCK_SUFFIX;
    }

    private static boolean isInsideScript(Document document, int offset) {
        CharSequence text = document.getCharsSequence();
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int scriptStart = lastIndexOfIgnoreCase(text, "<script", safeOffset);
        if (scriptStart < 0) return false;

        int scriptEnd = lastIndexOfIgnoreCase(text, "</script", safeOffset);
        if (scriptEnd > scriptStart) return false;

        int startTagEnd = indexOf(text, '>', scriptStart, safeOffset);
        return startTagEnd >= 0;
    }

    private static boolean matches(CharSequence text, int start, int end, String expected) {
        if (end - start != expected.length()) return false;
        for (int i = 0; i < expected.length(); i++) {
            if (text.charAt(start + i) != expected.charAt(i)) return false;
        }
        return true;
    }

    private static int lastIndexOfIgnoreCase(CharSequence text, String needle, int beforeOffset) {
        int maxStart = Math.min(beforeOffset, text.length()) - needle.length();
        for (int i = maxStart; i >= 0; i--) {
            if (startsWithIgnoreCase(text, i, needle)) return i;
        }
        return -1;
    }

    private static boolean startsWithIgnoreCase(CharSequence text, int offset, String needle) {
        if (offset + needle.length() > text.length()) return false;
        for (int i = 0; i < needle.length(); i++) {
            if (Character.toLowerCase(text.charAt(offset + i)) != Character.toLowerCase(needle.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(CharSequence text, char needle, int from, int to) {
        for (int i = Math.max(0, from); i < Math.min(to, text.length()); i++) {
            if (text.charAt(i) == needle) return i;
        }
        return -1;
    }

    public static final class CommentingState extends CommenterDataHolder {
        private final boolean script;

        private CommentingState(boolean script) {
            this.script = script;
        }
    }
}
