package com.github.codex.jsplite.lexer;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class JspLiteLexer extends LexerBase {
    private static final int TOP = 0;
    private static final int HTML_TAG = 1;
    private static final int HTML_SCRIPT_TAG = 2;
    private static final int HTML_STYLE_TAG = 3;
    private static final int JSP_DIRECTIVE = 4;
    private static final int JSP_SCRIPTLET = 5;
    private static final int JSP_EXPRESSION = 6;
    private static final int JSP_DECLARATION = 7;
    private static final int EL = 8;
    private static final int SCRIPT = 9;
    private static final int STYLE = 10;
    private static final int HTML_TAG_IN_SCRIPT = 11;
    private static final int HTML_TAG_IN_STYLE = 12;
    private static final int EL_IN_SCRIPT = 13;
    private static final int EL_IN_STYLE = 14;
    private static final int EL_IN_HTML_TAG = 15;
    private static final int EL_IN_HTML_SCRIPT_TAG = 16;
    private static final int EL_IN_HTML_STYLE_TAG = 17;
    private static final int EL_IN_HTML_TAG_IN_SCRIPT = 18;
    private static final int EL_IN_HTML_TAG_IN_STYLE = 19;
    private static final int JSP_DIRECTIVE_IN_SCRIPT = 20;
    private static final int JSP_SCRIPTLET_IN_SCRIPT = 21;
    private static final int JSP_EXPRESSION_IN_SCRIPT = 22;
    private static final int JSP_DECLARATION_IN_SCRIPT = 23;
    private static final int JSP_DIRECTIVE_IN_STYLE = 24;
    private static final int JSP_SCRIPTLET_IN_STYLE = 25;
    private static final int JSP_EXPRESSION_IN_STYLE = 26;
    private static final int JSP_DECLARATION_IN_STYLE = 27;
    private static final int JSP_DIRECTIVE_IN_HTML_TAG = 28;
    private static final int JSP_SCRIPTLET_IN_HTML_TAG = 29;
    private static final int JSP_EXPRESSION_IN_HTML_TAG = 30;
    private static final int JSP_DECLARATION_IN_HTML_TAG = 31;

    private static final Set<String> CODE_KEYWORDS = Set.of(
            "abstract", "as", "async", "await", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "debugger", "default", "delete", "do", "double", "else", "enum", "export",
            "extends", "false", "final", "finally", "float", "for", "from", "function", "get", "if", "implements",
            "import", "in", "instanceof", "int", "interface", "let", "long", "new", "null", "of", "package",
            "private", "protected", "public", "return", "set", "static", "super", "switch", "this", "throw",
            "throws", "true", "try", "typeof", "undefined", "var", "void", "while", "with", "yield"
    );

    private static final Set<String> EL_KEYWORDS = Set.of(
            "and", "div", "empty", "eq", "false", "ge", "gt", "le", "lt", "mod", "ne", "not", "null", "or", "true"
    );

    private static final Set<String> DECLARATION_PRECEDERS = Set.of(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void", "String", "Object",
            "Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Short", "List", "Map", "Set",
            "var", "let", "const"
    );

    private CharSequence buffer = "";
    private int startOffset;
    private int endOffset;
    private int tokenStart;
    private int tokenEnd;
    private int state;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.tokenStart = startOffset;
        this.tokenEnd = startOffset;
        this.state = normalizeState(initialState);
        locateToken();
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        tokenStart = tokenEnd;
        locateToken();
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    private void locateToken() {
        if (tokenStart >= endOffset) {
            tokenType = null;
            tokenEnd = tokenStart;
            return;
        }

        if (startsWith(tokenStart, "<%--")) {
            tokenType = JspLiteTokenTypes.JSP_COMMENT;
            tokenEnd = untilAfter(tokenStart + 4, "--%>");
            return;
        }
        if (startsWith(tokenStart, "<%@")) {
            tokenType = JspLiteTokenTypes.JSP_DIRECTIVE;
            tokenEnd = tokenStart + 3;
            state = jspStateFor(state, JSP_DIRECTIVE);
            return;
        }
        if (startsWith(tokenStart, "<%=")) {
            tokenType = JspLiteTokenTypes.JSP_EXPRESSION;
            tokenEnd = tokenStart + 3;
            state = jspStateFor(state, JSP_EXPRESSION);
            return;
        }
        if (startsWith(tokenStart, "<%!")) {
            tokenType = JspLiteTokenTypes.JSP_DECLARATION;
            tokenEnd = tokenStart + 3;
            state = jspStateFor(state, JSP_DECLARATION);
            return;
        }
        if (startsWith(tokenStart, "<%")) {
            tokenType = JspLiteTokenTypes.JSP_SCRIPTLET;
            tokenEnd = tokenStart + 2;
            state = jspStateFor(state, JSP_SCRIPTLET);
            return;
        }
        if (startsWith(tokenStart, "${") || startsWith(tokenStart, "#{")) {
            tokenType = JspLiteTokenTypes.EL_EXPRESSION;
            tokenEnd = tokenStart + 2;
            state = elStateFor(state);
            return;
        }

        switch (state) {
            case HTML_TAG:
            case HTML_SCRIPT_TAG:
            case HTML_STYLE_TAG:
            case HTML_TAG_IN_SCRIPT:
            case HTML_TAG_IN_STYLE:
                lexHtmlTag();
                break;
            case JSP_DIRECTIVE:
            case JSP_DIRECTIVE_IN_SCRIPT:
            case JSP_DIRECTIVE_IN_STYLE:
            case JSP_DIRECTIVE_IN_HTML_TAG:
                lexDirective(stateAfterJsp(state));
                break;
            case JSP_SCRIPTLET:
            case JSP_SCRIPTLET_IN_SCRIPT:
            case JSP_SCRIPTLET_IN_STYLE:
            case JSP_SCRIPTLET_IN_HTML_TAG:
                lexCode("%>", JspLiteTokenTypes.JSP_SCRIPTLET, CODE_KEYWORDS, stateAfterJsp(state));
                break;
            case JSP_EXPRESSION:
            case JSP_EXPRESSION_IN_SCRIPT:
            case JSP_EXPRESSION_IN_STYLE:
            case JSP_EXPRESSION_IN_HTML_TAG:
                lexCode("%>", JspLiteTokenTypes.JSP_EXPRESSION, CODE_KEYWORDS, stateAfterJsp(state));
                break;
            case JSP_DECLARATION:
            case JSP_DECLARATION_IN_SCRIPT:
            case JSP_DECLARATION_IN_STYLE:
            case JSP_DECLARATION_IN_HTML_TAG:
                lexCode("%>", JspLiteTokenTypes.JSP_DECLARATION, CODE_KEYWORDS, stateAfterJsp(state));
                break;
            case EL:
            case EL_IN_SCRIPT:
            case EL_IN_STYLE:
            case EL_IN_HTML_TAG:
            case EL_IN_HTML_SCRIPT_TAG:
            case EL_IN_HTML_STYLE_TAG:
            case EL_IN_HTML_TAG_IN_SCRIPT:
            case EL_IN_HTML_TAG_IN_STYLE:
                lexCode("}", JspLiteTokenTypes.EL_EXPRESSION, EL_KEYWORDS, stateAfterEl(state));
                break;
            case SCRIPT:
                lexScript();
                break;
            case STYLE:
                lexStyle();
                break;
            default:
                lexTop();
        }
    }

    private void lexTop() {
        if (startsWith(tokenStart, "<!--")) {
            tokenType = JspLiteTokenTypes.HTML_COMMENT;
            tokenEnd = untilAfter(tokenStart + 4, "-->");
            state = TOP;
            return;
        }
        if (startsWithIgnoreCase(tokenStart, "<!doctype")) {
            tokenType = JspLiteTokenTypes.DOCTYPE;
            tokenEnd = untilAfter(tokenStart + 2, ">");
            state = TOP;
            return;
        }
        char c = charAt(tokenStart);
        if (Character.isWhitespace(c)) {
            tokenType = JspLiteTokenTypes.WHITE_SPACE;
            tokenEnd = runWhitespace(tokenStart);
            state = TOP;
            return;
        }
        if (c == '<' && looksLikeTagStart(tokenStart)) {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            state = HTML_TAG;
            return;
        }
        if (looksLikeBareCodeLine(tokenStart)) {
            lexTopCode();
            state = TOP;
            return;
        }
        tokenType = JspLiteTokenTypes.TEXT;
        tokenEnd = nextTopTextBoundary(tokenStart + 1);
        state = TOP;
    }

    private void lexTopCode() {
        int boundary = currentLineEnd(tokenStart + 1);
        char c = charAt(tokenStart);
        if (startsWith(tokenStart, "//")) {
            tokenType = JspLiteTokenTypes.CODE_COMMENT;
            tokenEnd = Math.min(untilLineEnd(tokenStart + 2), boundary);
            return;
        }
        if (startsWith(tokenStart, "/*")) {
            int commentEnd = untilAfter(tokenStart + 2, "*/");
            tokenType = JspLiteTokenTypes.CODE_COMMENT;
            tokenEnd = Math.min(commentEnd, boundary);
            return;
        }
        if (c == '"' || c == '\'' || c == '`') {
            tokenType = JspLiteTokenTypes.STRING;
            tokenEnd = Math.min(quotedEnd(tokenStart, c), boundary);
            return;
        }
        if (Character.isDigit(c)) {
            tokenType = JspLiteTokenTypes.NUMBER;
            tokenEnd = Math.min(runNumber(tokenStart), boundary);
            return;
        }
        if (isNameStart(c)) {
            tokenEnd = Math.min(runCodeName(tokenStart), boundary);
            String word = slice(tokenStart, tokenEnd);
            tokenType = CODE_KEYWORDS.contains(word) ? JspLiteTokenTypes.KEYWORD : classifyCodeIdentifier(tokenStart, tokenEnd);
            return;
        }
        tokenType = JspLiteTokenTypes.OPERATOR;
        tokenEnd = Math.min(tokenStart + 1, boundary);
    }

    private void lexHtmlTag() {
        char c = charAt(tokenStart);
        if (Character.isWhitespace(c)) {
            tokenType = JspLiteTokenTypes.WHITE_SPACE;
            tokenEnd = runWhitespace(tokenStart);
            return;
        }
        if (c == '>') {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            if (state == HTML_SCRIPT_TAG) {
                state = isClosingTagEnd(tokenStart) ? TOP : SCRIPT;
            } else if (state == HTML_STYLE_TAG) {
                state = isClosingTagEnd(tokenStart) ? TOP : STYLE;
            } else if (state == HTML_TAG_IN_SCRIPT) {
                state = SCRIPT;
            } else if (state == HTML_TAG_IN_STYLE) {
                state = STYLE;
            } else {
                state = TOP;
            }
            return;
        }
        if (c == '<' && looksLikeTagStart(tokenStart)) {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            state = HTML_TAG;
            return;
        }
        if (c == '/' || c == '=') {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            return;
        }
        if (c == '"' || c == '\'') {
            tokenType = JspLiteTokenTypes.HTML_STRING;
            tokenEnd = quotedEnd(tokenStart, c);
            return;
        }
        if (isNameStart(c)) {
            tokenEnd = runName(tokenStart);
            String word = slice(tokenStart, tokenEnd);
            if (isTagName(tokenStart)) {
                tokenType = JspLiteTokenTypes.HTML_TAG;
                if ("script".equalsIgnoreCase(word)) state = HTML_SCRIPT_TAG;
                else if ("style".equalsIgnoreCase(word)) state = HTML_STYLE_TAG;
            } else {
                tokenType = JspLiteTokenTypes.HTML_ATTRIBUTE;
            }
            return;
        }
        tokenType = JspLiteTokenTypes.HTML_TAG;
        tokenEnd = tokenStart + 1;
    }

    private void lexDirective(int closeState) {
        if (startsWith(tokenStart, "%>")) {
            tokenType = JspLiteTokenTypes.JSP_DIRECTIVE;
            tokenEnd = tokenStart + 2;
            state = closeState;
            return;
        }
        char c = charAt(tokenStart);
        if (Character.isWhitespace(c)) {
            tokenType = JspLiteTokenTypes.WHITE_SPACE;
            tokenEnd = runWhitespace(tokenStart);
            return;
        }
        if (c == '"' || c == '\'') {
            tokenType = JspLiteTokenTypes.HTML_STRING;
            tokenEnd = quotedEnd(tokenStart, c);
            return;
        }
        if (c == '=' || c == '/' || c == '%' || c == '>') {
            tokenType = JspLiteTokenTypes.JSP_DIRECTIVE;
            tokenEnd = tokenStart + 1;
            return;
        }
        tokenEnd = Math.max(tokenStart + 1, runName(tokenStart));
        tokenType = previousNonWhitespace(tokenStart) == '@' ? JspLiteTokenTypes.JSP_DIRECTIVE : JspLiteTokenTypes.HTML_ATTRIBUTE;
    }

    private void lexScript() {
        if (startsWithIgnoreCase(tokenStart, "</script")) {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            state = HTML_TAG;
            return;
        }
        if (startsWith(tokenStart, "<!--")) {
            tokenType = JspLiteTokenTypes.CODE_COMMENT;
            tokenEnd = untilLineEnd(tokenStart + 4);
            state = SCRIPT;
            return;
        }
        if (charAt(tokenStart) == '<' && looksLikeTagStart(tokenStart)) {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            state = HTML_TAG_IN_SCRIPT;
            return;
        }
        lexCode("</script", JspLiteTokenTypes.IDENTIFIER, CODE_KEYWORDS, TOP);
        if (tokenType == JspLiteTokenTypes.IDENTIFIER) {
            tokenType = classifyCodeIdentifier(tokenStart, tokenEnd);
        }
        state = SCRIPT;
    }

    private void lexStyle() {
        if (startsWithIgnoreCase(tokenStart, "</style")) {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            state = HTML_TAG;
            return;
        }
        if (charAt(tokenStart) == '<' && looksLikeTagStart(tokenStart)) {
            tokenType = JspLiteTokenTypes.HTML_TAG;
            tokenEnd = tokenStart + 1;
            state = HTML_TAG_IN_STYLE;
            return;
        }
        char c = charAt(tokenStart);
        if (Character.isWhitespace(c)) {
            tokenType = JspLiteTokenTypes.WHITE_SPACE;
            tokenEnd = runWhitespace(tokenStart);
            state = STYLE;
            return;
        }
        if (startsWith(tokenStart, "/*")) {
            tokenType = JspLiteTokenTypes.CODE_COMMENT;
            tokenEnd = untilAfter(tokenStart + 2, "*/");
            state = STYLE;
            return;
        }
        if (c == '"' || c == '\'') {
            tokenType = JspLiteTokenTypes.STRING;
            tokenEnd = quotedEnd(tokenStart, c);
            state = STYLE;
            return;
        }
        if (Character.isDigit(c)) {
            tokenType = JspLiteTokenTypes.NUMBER;
            tokenEnd = runNumber(tokenStart);
            state = STYLE;
            return;
        }
        if (isCssNameStart(c)) {
            tokenEnd = runCssName(tokenStart);
            tokenType = classifyCssIdentifier(tokenStart, tokenEnd);
            state = STYLE;
            return;
        }
        tokenType = JspLiteTokenTypes.OPERATOR;
        tokenEnd = tokenStart + 1;
        state = STYLE;
    }

    private void lexCode(String closeMarker, IElementType fallbackType, Set<String> keywords, int closeState) {
        if (startsWithIgnoreCase(tokenStart, closeMarker)) {
            tokenType = fallbackType;
            tokenEnd = tokenStart + closeMarker.length();
            state = closeState;
            return;
        }
        char c = charAt(tokenStart);
        if (Character.isWhitespace(c)) {
            tokenType = JspLiteTokenTypes.WHITE_SPACE;
            tokenEnd = runWhitespace(tokenStart);
            return;
        }
        if (startsWith(tokenStart, "//")) {
            tokenType = JspLiteTokenTypes.CODE_COMMENT;
            tokenEnd = untilLineEnd(tokenStart + 2);
            return;
        }
        if (startsWith(tokenStart, "/*")) {
            tokenType = JspLiteTokenTypes.CODE_COMMENT;
            tokenEnd = untilAfterBeforeBoundary(tokenStart + 2, "*/", closeMarker);
            return;
        }
        if (c == '"' || c == '\'' || c == '`') {
            tokenType = JspLiteTokenTypes.STRING;
            tokenEnd = quotedEndBeforeBoundary(tokenStart, c, closeMarker);
            return;
        }
        if (Character.isDigit(c)) {
            tokenType = JspLiteTokenTypes.NUMBER;
            tokenEnd = runNumber(tokenStart);
            return;
        }
        if (isNameStart(c)) {
            tokenEnd = runCodeName(tokenStart);
            String word = slice(tokenStart, tokenEnd);
            tokenType = keywords.contains(word) ? JspLiteTokenTypes.KEYWORD : classifyCodeIdentifier(tokenStart, tokenEnd);
            return;
        }
        tokenType = JspLiteTokenTypes.OPERATOR;
        tokenEnd = tokenStart + 1;
    }

    private IElementType classifyCodeIdentifier(int from, int to) {
        char next = nextNonWhitespace(to);
        if (next == '=' && nextWordAfterAssignment(to).equals("function")) return JspLiteTokenTypes.FUNCTION;
        if (previousWordIs(from, "function") || next == '(') return JspLiteTokenTypes.FUNCTION;
        if (previousNonWhitespace(from) == '.') return JspLiteTokenTypes.PROPERTY;
        if (next == '.') return JspLiteTokenTypes.VARIABLE;
        if (next == ':') return JspLiteTokenTypes.VARIABLE;
        String previousWord = previousWordBefore(from);
        if (insideParameterList(from)) return JspLiteTokenTypes.PARAMETER;
        if (DECLARATION_PRECEDERS.contains(previousWord) || previousNonWhitespace(from) == ',') return JspLiteTokenTypes.VARIABLE;
        return JspLiteTokenTypes.VARIABLE;
    }

    private IElementType classifyCssIdentifier(int from, int to) {
        char next = nextNonWhitespace(to);
        char prev = previousNonWhitespace(from);
        if (next == ':' && prev != ':' && !insideCssSelector(from)) return JspLiteTokenTypes.CSS_PROPERTY;
        if (insideCssDeclaration(from)) return JspLiteTokenTypes.CSS_VALUE;
        return JspLiteTokenTypes.CSS_SELECTOR;
    }

    private boolean insideCssDeclaration(int offset) {
        for (int i = offset - 1; i >= startOffset; i--) {
            char c = buffer.charAt(i);
            if (c == ';' || c == '{') return c == ';' ? false : true;
            if (c == '}') return false;
        }
        return false;
    }

    private boolean insideCssSelector(int offset) {
        for (int i = offset - 1; i >= startOffset; i--) {
            char c = buffer.charAt(i);
            if (c == '{') return false;
            if (c == '}') return true;
        }
        return true;
    }

    private boolean insideParameterList(int offset) {
        int depth = 0;
        for (int i = offset - 1; i >= startOffset; i--) {
            char c = buffer.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') {
                if (depth == 0) return isDeclarationParameterList(i);
                depth--;
            } else if (c == '{' || c == ';') {
                return false;
            }
        }
        return false;
    }

    private boolean isDeclarationParameterList(int openParenOffset) {
        int nameStart = previousWordStartBefore(openParenOffset);
        if (nameStart < 0) return false;
        String declarationPreceder = previousWordBefore(nameStart);
        return "function".equals(declarationPreceder)
                || DECLARATION_PRECEDERS.contains(declarationPreceder)
                || isLikelyJavaType(declarationPreceder);
    }

    private boolean isLikelyJavaType(String word) {
        return !word.isEmpty() && Character.isUpperCase(word.charAt(0));
    }

    private String nextWordAfterAssignment(int offset) {
        int i = offset;
        while (i < endOffset && Character.isWhitespace(buffer.charAt(i))) i++;
        if (i >= endOffset || buffer.charAt(i) != '=') return "";
        i++;
        while (i < endOffset && Character.isWhitespace(buffer.charAt(i))) i++;
        int start = i;
        while (i < endOffset && isNamePart(buffer.charAt(i))) i++;
        return start < i ? slice(start, i) : "";
    }

    private boolean previousWordIs(int offset, String expected) {
        return previousWordBefore(offset).equals(expected);
    }

    private String previousWordBefore(int offset) {
        int start = previousWordStartBefore(offset);
        if (start < 0) return "";
        int end = start;
        while (end < endOffset && isNamePart(buffer.charAt(end))) end++;
        return slice(start, end);
    }

    private int previousWordStartBefore(int offset) {
        int i = offset - 1;
        while (i >= startOffset && Character.isWhitespace(buffer.charAt(i))) i--;
        while (i >= startOffset && !isNamePart(buffer.charAt(i))) i--;
        if (i < startOffset) return -1;
        while (i >= startOffset && isNamePart(buffer.charAt(i))) i--;
        return i + 1;
    }

    private int normalizeState(int value) {
        return value >= TOP && value <= JSP_DECLARATION_IN_HTML_TAG ? value : TOP;
    }

    private int elStateFor(int currentState) {
        switch (currentState) {
            case SCRIPT:
                return EL_IN_SCRIPT;
            case STYLE:
                return EL_IN_STYLE;
            case HTML_TAG:
                return EL_IN_HTML_TAG;
            case HTML_SCRIPT_TAG:
                return EL_IN_HTML_SCRIPT_TAG;
            case HTML_STYLE_TAG:
                return EL_IN_HTML_STYLE_TAG;
            case HTML_TAG_IN_SCRIPT:
                return EL_IN_HTML_TAG_IN_SCRIPT;
            case HTML_TAG_IN_STYLE:
                return EL_IN_HTML_TAG_IN_STYLE;
            default:
                return EL;
        }
    }

    private int stateAfterEl(int elState) {
        switch (elState) {
            case EL_IN_SCRIPT:
                return SCRIPT;
            case EL_IN_STYLE:
                return STYLE;
            case EL_IN_HTML_TAG:
                return HTML_TAG;
            case EL_IN_HTML_SCRIPT_TAG:
                return HTML_SCRIPT_TAG;
            case EL_IN_HTML_STYLE_TAG:
                return HTML_STYLE_TAG;
            case EL_IN_HTML_TAG_IN_SCRIPT:
                return HTML_TAG_IN_SCRIPT;
            case EL_IN_HTML_TAG_IN_STYLE:
                return HTML_TAG_IN_STYLE;
            default:
                return TOP;
        }
    }

    private int jspStateFor(int currentState, int jspState) {
        if (currentState == SCRIPT) {
            switch (jspState) {
                case JSP_DIRECTIVE:
                    return JSP_DIRECTIVE_IN_SCRIPT;
                case JSP_EXPRESSION:
                    return JSP_EXPRESSION_IN_SCRIPT;
                case JSP_DECLARATION:
                    return JSP_DECLARATION_IN_SCRIPT;
                default:
                    return JSP_SCRIPTLET_IN_SCRIPT;
            }
        }
        if (currentState == STYLE) {
            switch (jspState) {
                case JSP_DIRECTIVE:
                    return JSP_DIRECTIVE_IN_STYLE;
                case JSP_EXPRESSION:
                    return JSP_EXPRESSION_IN_STYLE;
                case JSP_DECLARATION:
                    return JSP_DECLARATION_IN_STYLE;
                default:
                    return JSP_SCRIPTLET_IN_STYLE;
            }
        }
        switch (currentState) {
            case HTML_TAG:
            case HTML_SCRIPT_TAG:
            case HTML_STYLE_TAG:
            case HTML_TAG_IN_SCRIPT:
            case HTML_TAG_IN_STYLE:
                switch (jspState) {
                    case JSP_DIRECTIVE:
                        return JSP_DIRECTIVE_IN_HTML_TAG;
                    case JSP_EXPRESSION:
                        return JSP_EXPRESSION_IN_HTML_TAG;
                    case JSP_DECLARATION:
                        return JSP_DECLARATION_IN_HTML_TAG;
                    default:
                        return JSP_SCRIPTLET_IN_HTML_TAG;
                }
            default:
                return jspState;
        }
    }

    private int stateAfterJsp(int jspState) {
        switch (jspState) {
            case JSP_DIRECTIVE_IN_SCRIPT:
            case JSP_SCRIPTLET_IN_SCRIPT:
            case JSP_EXPRESSION_IN_SCRIPT:
            case JSP_DECLARATION_IN_SCRIPT:
                return SCRIPT;
            case JSP_DIRECTIVE_IN_STYLE:
            case JSP_SCRIPTLET_IN_STYLE:
            case JSP_EXPRESSION_IN_STYLE:
            case JSP_DECLARATION_IN_STYLE:
                return STYLE;
            case JSP_DIRECTIVE_IN_HTML_TAG:
            case JSP_SCRIPTLET_IN_HTML_TAG:
            case JSP_EXPRESSION_IN_HTML_TAG:
            case JSP_DECLARATION_IN_HTML_TAG:
                return HTML_TAG;
            default:
                return TOP;
        }
    }

    private boolean startsWith(int offset, String value) {
        if (offset + value.length() > endOffset) return false;
        for (int i = 0; i < value.length(); i++) {
            if (buffer.charAt(offset + i) != value.charAt(i)) return false;
        }
        return true;
    }

    private boolean startsWithIgnoreCase(int offset, String value) {
        if (offset + value.length() > endOffset) return false;
        for (int i = 0; i < value.length(); i++) {
            if (Character.toLowerCase(buffer.charAt(offset + i)) != Character.toLowerCase(value.charAt(i))) return false;
        }
        return true;
    }

    private int indexOf(int from, String needle) {
        outer:
        for (int i = from; i <= endOffset - needle.length(); i++) {
            for (int j = 0; j < needle.length(); j++) {
                if (buffer.charAt(i + j) != needle.charAt(j)) continue outer;
            }
            return i;
        }
        return -1;
    }

    private int untilAfter(int from, String needle) {
        int found = indexOf(from, needle);
        return found < 0 ? endOffset : found + needle.length();
    }

    private int untilAfterBeforeBoundary(int from, String needle, String boundary) {
        int found = indexOf(from, needle);
        int boundaryOffset = indexOfIgnoreCase(from, boundary);
        if (found >= 0 && (boundaryOffset < 0 || found < boundaryOffset)) return found + needle.length();
        if (boundaryOffset >= 0) return boundaryOffset;
        return endOffset;
    }

    private int untilLineEnd(int from) {
        return currentLineEnd(from);
    }

    private int currentLineEnd(int from) {
        for (int i = from; i < endOffset; i++) {
            char c = buffer.charAt(i);
            if (c == '\n' || c == '\r') return i;
        }
        return endOffset;
    }

    private int nextTopTextBoundary(int from) {
        for (int i = from; i < endOffset; i++) {
            char c = buffer.charAt(i);
            if (c == '\n' || c == '\r') return i;
            if (c == '<' || (c == '$' && i + 1 < endOffset && buffer.charAt(i + 1) == '{')
                    || (c == '#' && i + 1 < endOffset && buffer.charAt(i + 1) == '{')) return i;
        }
        return endOffset;
    }

    private boolean looksLikeBareCodeLine(int offset) {
        int lineStart = offset;
        while (lineStart > startOffset) {
            char c = buffer.charAt(lineStart - 1);
            if (c == '\n' || c == '\r') break;
            lineStart--;
        }
        int lineEnd = offset;
        while (lineEnd < endOffset) {
            char c = buffer.charAt(lineEnd);
            if (c == '\n' || c == '\r' || c == '<') break;
            lineEnd++;
        }
        String line = buffer.subSequence(lineStart, lineEnd).toString().trim();
        if (line.isEmpty()) return false;
        if (line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) return true;
        if (line.startsWith("function ") || line.startsWith("var ") || line.startsWith("let ") || line.startsWith("const ")) return true;
        if (line.equals("debugger") || line.equals("debugger;")) return true;
        if (line.startsWith("if ") || line.startsWith("if(") || line.startsWith("for ") || line.startsWith("for(")
                || line.startsWith("while ") || line.startsWith("while(") || line.startsWith("switch ") || line.startsWith("switch(")
                || line.startsWith("return ") || line.startsWith("try") || line.startsWith("catch ") || line.startsWith("catch(")) {
            return true;
        }
        if (line.startsWith("}") || line.startsWith("{") || line.startsWith("]") || line.endsWith(";")
                || line.endsWith("{") || line.endsWith("}") || line.endsWith(",")) return true;
        if (isObjectLiteralLine(line)) return true;
        return line.contains("=") || line.contains("(") || line.contains(")") || line.contains("document.")
                || line.contains("window.") || line.contains("console.") || line.contains("$.") || line.contains("$(");
    }

    private boolean isObjectLiteralLine(String line) {
        int colon = line.indexOf(':');
        if (colon <= 0) return false;
        String key = line.substring(0, colon).trim();
        if (key.isEmpty()) return false;
        if ((key.startsWith("\"") && key.endsWith("\"")) || (key.startsWith("'") && key.endsWith("'"))) return true;
        return isNameStart(key.charAt(0)) && key.chars().allMatch(ch -> isCodeNamePart((char) ch));
    }

    private boolean looksLikeTagStart(int offset) {
        if (offset + 1 >= endOffset) return false;
        char c = charAt(offset + 1);
        return c == '/' || c == '!' || isNameStart(c);
    }

    private boolean isTagName(int offset) {
        char prev = previousNonWhitespace(offset);
        return prev == '<' || prev == '/';
    }

    private boolean isClosingTagEnd(int offset) {
        for (int i = offset - 1; i >= startOffset; i--) {
            char c = buffer.charAt(i);
            if (c == '<') return i + 1 < offset && buffer.charAt(i + 1) == '/';
            if (c == '>') return false;
        }
        return false;
    }

    private int quotedEnd(int offset, char quote) {
        for (int i = offset + 1; i < endOffset; i++) {
            if (buffer.charAt(i) == quote && buffer.charAt(Math.max(startOffset, i - 1)) != '\\') return i + 1;
        }
        return endOffset;
    }

    private int quotedEndBeforeBoundary(int offset, char quote, String boundary) {
        for (int i = offset + 1; i < endOffset; i++) {
            char c = buffer.charAt(i);
            if (startsWithIgnoreCase(i, boundary)) return i;
            if (c == quote && buffer.charAt(Math.max(startOffset, i - 1)) != '\\') return i + 1;
            if (quote != '`' && (c == '\n' || c == '\r')) return i;
        }
        return endOffset;
    }

    private int indexOfIgnoreCase(int from, String needle) {
        outer:
        for (int i = from; i <= endOffset - needle.length(); i++) {
            for (int j = 0; j < needle.length(); j++) {
                if (Character.toLowerCase(buffer.charAt(i + j)) != Character.toLowerCase(needle.charAt(j))) continue outer;
            }
            return i;
        }
        return -1;
    }

    private int runWhitespace(int offset) {
        int i = offset;
        while (i < endOffset && Character.isWhitespace(buffer.charAt(i))) i++;
        return i;
    }

    private int runName(int offset) {
        int i = offset;
        while (i < endOffset && isNamePart(buffer.charAt(i))) i++;
        return i;
    }

    private int runCodeName(int offset) {
        int i = offset;
        while (i < endOffset && isCodeNamePart(buffer.charAt(i))) i++;
        return i;
    }

    private int runCssName(int offset) {
        int i = offset;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (!(isNamePart(c) || c == '.' || c == '#' || c == '%' || c == '@')) break;
            i++;
        }
        return i;
    }

    private int runNumber(int offset) {
        int i = offset;
        while (i < endOffset) {
            char c = buffer.charAt(i);
            if (!(Character.isDigit(c) || c == '.' || c == '_' || Character.isLetter(c) || c == '%' || c == '-')) break;
            i++;
        }
        return i;
    }

    private char previousNonWhitespace(int offset) {
        for (int i = offset - 1; i >= startOffset; i--) {
            char c = buffer.charAt(i);
            if (!Character.isWhitespace(c)) return c;
        }
        return '\0';
    }

    private char nextNonWhitespace(int offset) {
        for (int i = offset; i < endOffset; i++) {
            char c = buffer.charAt(i);
            if (!Character.isWhitespace(c)) return c;
        }
        return '\0';
    }

    private boolean isNameStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isCssNameStart(char c) {
        return isNameStart(c) || c == '.' || c == '#' || c == '@' || c == '-';
    }

    private boolean isNamePart(char c) {
        return isNameStart(c) || Character.isDigit(c) || c == ':' || c == '.' || c == '-';
    }

    private boolean isCodeNamePart(char c) {
        return isNameStart(c) || Character.isDigit(c);
    }

    private String slice(int from, int to) {
        return buffer.subSequence(from, to).toString();
    }

    private char charAt(int offset) {
        return buffer.charAt(offset);
    }
}
