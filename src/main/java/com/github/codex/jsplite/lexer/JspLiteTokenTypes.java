package com.github.codex.jsplite.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;

public interface JspLiteTokenTypes {
    IElementType TEXT = new JspLiteTokenType("TEXT");
    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    IElementType JSP_COMMENT = new JspLiteTokenType("JSP_COMMENT");
    IElementType JSP_DIRECTIVE = new JspLiteTokenType("JSP_DIRECTIVE");
    IElementType JSP_DECLARATION = new JspLiteTokenType("JSP_DECLARATION");
    IElementType JSP_EXPRESSION = new JspLiteTokenType("JSP_EXPRESSION");
    IElementType JSP_SCRIPTLET = new JspLiteTokenType("JSP_SCRIPTLET");
    IElementType EL_EXPRESSION = new JspLiteTokenType("EL_EXPRESSION");
    IElementType HTML_TAG = new JspLiteTokenType("HTML_TAG");
    IElementType HTML_ATTRIBUTE = new JspLiteTokenType("HTML_ATTRIBUTE");
    IElementType HTML_STRING = new JspLiteTokenType("HTML_STRING");
    IElementType HTML_COMMENT = new JspLiteTokenType("HTML_COMMENT");
    IElementType DOCTYPE = new JspLiteTokenType("DOCTYPE");
    IElementType KEYWORD = new JspLiteTokenType("KEYWORD");
    IElementType IDENTIFIER = new JspLiteTokenType("IDENTIFIER");
    IElementType FUNCTION = new JspLiteTokenType("FUNCTION");
    IElementType VARIABLE = new JspLiteTokenType("VARIABLE");
    IElementType PARAMETER = new JspLiteTokenType("PARAMETER");
    IElementType PROPERTY = new JspLiteTokenType("PROPERTY");
    IElementType CSS_SELECTOR = new JspLiteTokenType("CSS_SELECTOR");
    IElementType CSS_PROPERTY = new JspLiteTokenType("CSS_PROPERTY");
    IElementType CSS_VALUE = new JspLiteTokenType("CSS_VALUE");
    IElementType NUMBER = new JspLiteTokenType("NUMBER");
    IElementType STRING = new JspLiteTokenType("STRING");
    IElementType OPERATOR = new JspLiteTokenType("OPERATOR");
    IElementType CODE_COMMENT = new JspLiteTokenType("CODE_COMMENT");
    IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;
}
