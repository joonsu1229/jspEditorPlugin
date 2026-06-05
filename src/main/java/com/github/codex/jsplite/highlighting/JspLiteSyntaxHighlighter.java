package com.github.codex.jsplite.highlighting;

import com.github.codex.jsplite.lexer.JspLiteLexer;
import com.github.codex.jsplite.lexer.JspLiteTokenTypes;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

public final class JspLiteSyntaxHighlighter implements SyntaxHighlighter {
    public static final TextAttributesKey JSP_DIRECTIVE = key("JSP_LITE_DIRECTIVE_COLOR", 0x7A6F00, 0xBBB529, DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey JSP_SCRIPTLET = key("JSP_LITE_SCRIPTLET_COLOR", 0xB15C00, 0xCC7832, DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey JSP_EXPRESSION = key("JSP_LITE_EXPRESSION_COLOR", 0x005CC5, 0x82AAFF, DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    public static final TextAttributesKey JSP_COMMENT = key("JSP_LITE_COMMENT_COLOR", 0x6A737D, 0x808080, DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey EL_EXPRESSION = key("JSP_LITE_EL_COLOR", 0x067D17, 0x6A8759, DefaultLanguageHighlighterColors.STATIC_FIELD);
    public static final TextAttributesKey HTML_TAG = key("JSP_LITE_HTML_TAG_COLOR", 0x0F4B99, 0xE8BF6A, DefaultLanguageHighlighterColors.MARKUP_TAG);
    public static final TextAttributesKey HTML_ATTRIBUTE = key("JSP_LITE_HTML_ATTRIBUTE_COLOR", 0x871094, 0xC792EA, DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE);
    public static final TextAttributesKey HTML_STRING = key("JSP_LITE_HTML_STRING_COLOR", 0x067D17, 0x6A8759, DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey HTML_COMMENT = key("JSP_LITE_HTML_COMMENT_COLOR", 0x6A737D, 0x808080, DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey KEYWORD = key("JSP_LITE_KEYWORD_COLOR", 0xB15C00, 0xCC7832, DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey IDENTIFIER = key("JSP_LITE_IDENTIFIER_COLOR", 0x24292F, 0xA9B7C6, DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey FUNCTION = key("JSP_LITE_FUNCTION_COLOR", 0x795E00, 0xFFC66D, DefaultLanguageHighlighterColors.FUNCTION_CALL);
    public static final TextAttributesKey VARIABLE = key("JSP_LITE_VARIABLE_COLOR", 0x005CC5, 0x82AAFF, DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    public static final TextAttributesKey PARAMETER = key("JSP_LITE_PARAMETER_COLOR", 0x8B3A99, 0xC586C0, DefaultLanguageHighlighterColors.PARAMETER);
    public static final TextAttributesKey PROPERTY = key("JSP_LITE_PROPERTY_COLOR", 0x4B5563, 0xA9B7C6, DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey CSS_SELECTOR = key("JSP_LITE_CSS_SELECTOR_COLOR", 0x00627A, 0x89DDFF, DefaultLanguageHighlighterColors.MARKUP_TAG);
    public static final TextAttributesKey CSS_PROPERTY = key("JSP_LITE_CSS_PROPERTY_COLOR", 0x871094, 0xC792EA, DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey CSS_VALUE = key("JSP_LITE_CSS_VALUE_COLOR", 0x067D17, 0x6A8759, DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey NUMBER = key("JSP_LITE_NUMBER_COLOR", 0x1750EB, 0x6897BB, DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING = key("JSP_LITE_STRING_COLOR", 0x067D17, 0x6A8759, DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey OPERATOR = key("JSP_LITE_OPERATOR_COLOR", 0x24292F, 0xA9B7C6, DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey CODE_COMMENT = key("JSP_LITE_CODE_COMMENT_COLOR", 0x6A737D, 0x808080, DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("JSP_LITE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

    private static final TextAttributesKey[] EMPTY = new TextAttributesKey[0];
    private static final TextAttributesKey[] JSP_DIRECTIVE_KEYS = keys(JSP_DIRECTIVE);
    private static final TextAttributesKey[] JSP_SCRIPTLET_KEYS = keys(JSP_SCRIPTLET);
    private static final TextAttributesKey[] JSP_EXPRESSION_KEYS = keys(JSP_EXPRESSION);
    private static final TextAttributesKey[] JSP_COMMENT_KEYS = keys(JSP_COMMENT);
    private static final TextAttributesKey[] EL_KEYS = keys(EL_EXPRESSION);
    private static final TextAttributesKey[] HTML_TAG_KEYS = keys(HTML_TAG);
    private static final TextAttributesKey[] HTML_ATTRIBUTE_KEYS = keys(HTML_ATTRIBUTE);
    private static final TextAttributesKey[] HTML_STRING_KEYS = keys(HTML_STRING);
    private static final TextAttributesKey[] HTML_COMMENT_KEYS = keys(HTML_COMMENT);
    private static final TextAttributesKey[] KEYWORD_KEYS = keys(KEYWORD);
    private static final TextAttributesKey[] IDENTIFIER_KEYS = keys(IDENTIFIER);
    private static final TextAttributesKey[] FUNCTION_KEYS = keys(FUNCTION);
    private static final TextAttributesKey[] VARIABLE_KEYS = keys(VARIABLE);
    private static final TextAttributesKey[] PARAMETER_KEYS = keys(PARAMETER);
    private static final TextAttributesKey[] PROPERTY_KEYS = keys(PROPERTY);
    private static final TextAttributesKey[] CSS_SELECTOR_KEYS = keys(CSS_SELECTOR);
    private static final TextAttributesKey[] CSS_PROPERTY_KEYS = keys(CSS_PROPERTY);
    private static final TextAttributesKey[] CSS_VALUE_KEYS = keys(CSS_VALUE);
    private static final TextAttributesKey[] NUMBER_KEYS = keys(NUMBER);
    private static final TextAttributesKey[] STRING_KEYS = keys(STRING);
    private static final TextAttributesKey[] OPERATOR_KEYS = keys(OPERATOR);
    private static final TextAttributesKey[] CODE_COMMENT_KEYS = keys(CODE_COMMENT);
    private static final TextAttributesKey[] BAD_CHARACTER_KEYS = keys(BAD_CHARACTER);

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new JspLiteLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == JspLiteTokenTypes.JSP_DIRECTIVE) return JSP_DIRECTIVE_KEYS;
        if (tokenType == JspLiteTokenTypes.JSP_SCRIPTLET || tokenType == JspLiteTokenTypes.JSP_DECLARATION) return JSP_SCRIPTLET_KEYS;
        if (tokenType == JspLiteTokenTypes.JSP_EXPRESSION) return JSP_EXPRESSION_KEYS;
        if (tokenType == JspLiteTokenTypes.JSP_COMMENT) return JSP_COMMENT_KEYS;
        if (tokenType == JspLiteTokenTypes.EL_EXPRESSION) return EL_KEYS;
        if (tokenType == JspLiteTokenTypes.HTML_TAG || tokenType == JspLiteTokenTypes.DOCTYPE) return HTML_TAG_KEYS;
        if (tokenType == JspLiteTokenTypes.HTML_ATTRIBUTE) return HTML_ATTRIBUTE_KEYS;
        if (tokenType == JspLiteTokenTypes.HTML_STRING) return HTML_STRING_KEYS;
        if (tokenType == JspLiteTokenTypes.HTML_COMMENT) return HTML_COMMENT_KEYS;
        if (tokenType == JspLiteTokenTypes.KEYWORD) return KEYWORD_KEYS;
        if (tokenType == JspLiteTokenTypes.IDENTIFIER) return IDENTIFIER_KEYS;
        if (tokenType == JspLiteTokenTypes.FUNCTION) return FUNCTION_KEYS;
        if (tokenType == JspLiteTokenTypes.VARIABLE) return VARIABLE_KEYS;
        if (tokenType == JspLiteTokenTypes.PARAMETER) return PARAMETER_KEYS;
        if (tokenType == JspLiteTokenTypes.PROPERTY) return PROPERTY_KEYS;
        if (tokenType == JspLiteTokenTypes.CSS_SELECTOR) return CSS_SELECTOR_KEYS;
        if (tokenType == JspLiteTokenTypes.CSS_PROPERTY) return CSS_PROPERTY_KEYS;
        if (tokenType == JspLiteTokenTypes.CSS_VALUE) return CSS_VALUE_KEYS;
        if (tokenType == JspLiteTokenTypes.NUMBER) return NUMBER_KEYS;
        if (tokenType == JspLiteTokenTypes.STRING) return STRING_KEYS;
        if (tokenType == JspLiteTokenTypes.OPERATOR) return OPERATOR_KEYS;
        if (tokenType == JspLiteTokenTypes.CODE_COMMENT) return CODE_COMMENT_KEYS;
        if (tokenType == JspLiteTokenTypes.BAD_CHARACTER) return BAD_CHARACTER_KEYS;
        return EMPTY;
    }

    private static TextAttributesKey[] keys(TextAttributesKey key) {
        return new TextAttributesKey[]{key};
    }

    private static TextAttributesKey key(String externalName, int lightRgb, int darkRgb, TextAttributesKey fallbackKey) {
        TextAttributes attributes = new TextAttributes();
        attributes.setForegroundColor(new JBColor(new Color(lightRgb), new Color(darkRgb)));
        TextAttributesKey key = createTextAttributesKey(externalName, attributes);
        key.setFallbackAttributeKey(fallbackKey);
        return key;
    }
}
