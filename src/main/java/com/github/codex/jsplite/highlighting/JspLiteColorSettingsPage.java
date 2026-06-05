package com.github.codex.jsplite.highlighting;

import com.github.codex.jsplite.JspLiteIcons;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Map;

public final class JspLiteColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("JSP directive", JspLiteSyntaxHighlighter.JSP_DIRECTIVE),
            new AttributesDescriptor("JSP scriptlet/declaration", JspLiteSyntaxHighlighter.JSP_SCRIPTLET),
            new AttributesDescriptor("JSP expression", JspLiteSyntaxHighlighter.JSP_EXPRESSION),
            new AttributesDescriptor("JSP comment", JspLiteSyntaxHighlighter.JSP_COMMENT),
            new AttributesDescriptor("EL expression", JspLiteSyntaxHighlighter.EL_EXPRESSION),
            new AttributesDescriptor("HTML tag", JspLiteSyntaxHighlighter.HTML_TAG),
            new AttributesDescriptor("HTML attribute", JspLiteSyntaxHighlighter.HTML_ATTRIBUTE),
            new AttributesDescriptor("HTML string", JspLiteSyntaxHighlighter.HTML_STRING),
            new AttributesDescriptor("HTML comment", JspLiteSyntaxHighlighter.HTML_COMMENT),
            new AttributesDescriptor("Code keyword", JspLiteSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Code identifier", JspLiteSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Code function", JspLiteSyntaxHighlighter.FUNCTION),
            new AttributesDescriptor("Code variable", JspLiteSyntaxHighlighter.VARIABLE),
            new AttributesDescriptor("Code parameter", JspLiteSyntaxHighlighter.PARAMETER),
            new AttributesDescriptor("Code property", JspLiteSyntaxHighlighter.PROPERTY),
            new AttributesDescriptor("CSS selector", JspLiteSyntaxHighlighter.CSS_SELECTOR),
            new AttributesDescriptor("CSS property", JspLiteSyntaxHighlighter.CSS_PROPERTY),
            new AttributesDescriptor("CSS value", JspLiteSyntaxHighlighter.CSS_VALUE),
            new AttributesDescriptor("Code number", JspLiteSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("Code string", JspLiteSyntaxHighlighter.STRING),
            new AttributesDescriptor("Code operator", JspLiteSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Code comment", JspLiteSyntaxHighlighter.CODE_COMMENT),
            new AttributesDescriptor("Bad character", JspLiteSyntaxHighlighter.BAD_CHARACTER)
    };

    @Override
    public @Nullable Icon getIcon() {
        return JspLiteIcons.FILE;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new JspLiteSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        return "<%@ page contentType=\"text/html; charset=UTF-8\" pageEncoding=\"UTF-8\" %>\n"
                + "<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>\n"
                + "<%-- JSP comment --%>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <style>\n"
                + "    .user-name, #profileCard { color: #305080; margin-top: 12px; }\n"
                + "  </style>\n"
                + "  <script>\n"
                + "    function renderUser(userName, index) {\n"
                + "      const user = \"${sessionScope.user.name}\";\n"
                + "      if (user) console.log(userName, index);\n"
                + "    }\n"
                + "  </script>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <c:if test=\"${not empty sessionScope.user}\">\n"
                + "    <% String path = request.getContextPath(); render(path); %>\n"
                + "    <%= path.toLowerCase() %>\n"
                + "  </c:if>\n"
                + "</body>\n"
                + "</html>\n";
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "JSP Lite";
    }
}
