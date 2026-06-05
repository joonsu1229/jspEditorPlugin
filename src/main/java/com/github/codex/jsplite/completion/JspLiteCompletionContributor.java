package com.github.codex.jsplite.completion;

import com.github.codex.jsplite.JspLiteFileType;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class JspLiteCompletionContributor extends CompletionContributor {
    private static final int CONTEXT_LIMIT = 180;
    private static final int TAGLIB_SCAN_LIMIT = 64_000;

    private static final List<String> DIRECTIVES = List.of(
            "<%@ page contentType=\"text/html; charset=UTF-8\" pageEncoding=\"UTF-8\" %>",
            "<%@ include file=\"\" %>",
            "<%@ taglib prefix=\"c\" uri=\"http://java.sun.com/jsp/jstl/core\" %>",
            "<%@ taglib prefix=\"fmt\" uri=\"http://java.sun.com/jsp/jstl/fmt\" %>",
            "<%@ taglib prefix=\"fn\" uri=\"http://java.sun.com/jsp/jstl/functions\" %>"
    );

    private static final List<String> JSTL_CORE = List.of(
            "c:out", "c:set", "c:remove", "c:catch", "c:if", "c:choose", "c:when", "c:otherwise",
            "c:forEach", "c:forTokens", "c:import", "c:url", "c:param", "c:redirect"
    );

    private static final List<String> JSTL_FMT = List.of(
            "fmt:formatNumber", "fmt:parseNumber", "fmt:formatDate", "fmt:parseDate", "fmt:bundle",
            "fmt:setBundle", "fmt:message", "fmt:param", "fmt:setLocale", "fmt:setTimeZone", "fmt:timeZone"
    );

    private static final List<String> JSP_TAGS = List.of("jsp:include", "jsp:forward", "jsp:param");

    private static final List<String> EL_OBJECTS = List.of(
            "pageContext", "pageScope", "requestScope", "sessionScope", "applicationScope",
            "param", "paramValues", "header", "headerValues", "cookie", "initParam"
    );

    private static final List<String> DIRECTIVE_ATTRIBUTES = List.of(
            "contentType", "pageEncoding", "language", "import", "session", "buffer", "autoFlush",
            "isThreadSafe", "info", "errorPage", "isErrorPage", "isELIgnored", "deferredSyntaxAllowedAsLiteral",
            "trimDirectiveWhitespaces", "file", "prefix", "uri", "tagdir"
    );

    private static final List<String> ATTRIBUTES = List.of(
            "id", "class", "style", "name", "value", "type", "href", "src", "alt", "title",
            "method", "action", "for", "page", "file", "test", "items", "var", "begin", "end", "step", "scope"
    );

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (parameters.getOriginalFile().getFileType() != JspLiteFileType.INSTANCE) {
            super.fillCompletionVariants(parameters, result);
            return;
        }

        Document document = parameters.getEditor().getDocument();
        CharSequence text = document.getCharsSequence();
        int offset = Math.max(0, Math.min(parameters.getOffset(), text.length()));
        String before = text.subSequence(Math.max(0, offset - CONTEXT_LIMIT), offset).toString();

        if (before.endsWith("<%@") || before.contains("<%@")) {
            addAll(result, DIRECTIVES, "JSP directive");
            addAll(result, DIRECTIVE_ATTRIBUTES, "JSP directive attribute");
        }

        if (insideEl(before)) {
            addAll(result, EL_OBJECTS, "EL implicit object");
            addAll(result, List.of("empty", "not", "and", "or", "eq", "ne", "lt", "le", "gt", "ge"), "EL operator");
            return;
        }

        if (before.endsWith("<") || before.endsWith("</") || before.matches("(?s).*<\\w*$")) {
            for (String tag : visibleTags(text, before)) {
                addTag(result, tag, commonTail(tag));
            }
        }

        if (insideTag(before)) {
            addAll(result, ATTRIBUTES, "Attribute");
        }

        addAll(result, List.of("<% %>", "<%= %>", "<%! %>", "<%-- --%>", "${}"), "JSP snippet");
    }

    private static void addAll(CompletionResultSet result, List<String> values, String type) {
        for (String value : values) {
            result.addElement(LookupElementBuilder.create(value).withTypeText(type, true).withIcon(AllIcons.FileTypes.Html));
        }
    }

    private static void addTag(CompletionResultSet result, String tag, String attrs) {
        String insert = attrs.isBlank() ? tag + ">" : tag + " " + attrs + "=\"\">";
        result.addElement(LookupElementBuilder.create(tag)
                .withInsertHandler((context, item) -> {
                    context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), insert);
                    context.getEditor().getCaretModel().moveToOffset(context.getStartOffset() + insert.length() - 2);
                })
                .withTypeText("JSP/JSTL tag", true)
                .withIcon(AllIcons.Nodes.Tag));
    }

    private static List<String> visibleTags(CharSequence text, String before) {
        Set<String> tags = new LinkedHashSet<>(JSP_TAGS);
        String prefix = currentTagPrefix(before);
        DeclaredPrefixes prefixes = declaredPrefixes(text);

        if (prefix.isEmpty() || prefixes.corePrefix.equals(prefix) || "c".equals(prefix)) {
            addTags(tags, JSTL_CORE, prefixes.corePrefix);
        }
        if (prefix.isEmpty() || prefixes.fmtPrefix.equals(prefix) || "fmt".equals(prefix)) {
            addTags(tags, JSTL_FMT, prefixes.fmtPrefix);
        }
        return new ArrayList<>(tags);
    }

    private static void addTags(Set<String> tags, List<String> source, String prefix) {
        for (String tag : source) {
            int colon = tag.indexOf(':');
            tags.add(colon < 0 ? tag : prefix + tag.substring(colon));
        }
    }

    private static DeclaredPrefixes declaredPrefixes(CharSequence text) {
        int length = Math.min(text.length(), TAGLIB_SCAN_LIMIT);
        String head = text.subSequence(0, length).toString();
        String core = declaredPrefix(head, "http://java.sun.com/jsp/jstl/core", "c");
        if (core.equals("c")) core = declaredPrefix(head, "jakarta.tags.core", "c");
        String fmt = declaredPrefix(head, "http://java.sun.com/jsp/jstl/fmt", "fmt");
        if (fmt.equals("fmt")) fmt = declaredPrefix(head, "jakarta.tags.fmt", "fmt");
        return new DeclaredPrefixes(core, fmt);
    }

    private static String declaredPrefix(String head, String uri, String prefix) {
        int uriAt = head.indexOf(uri);
        if (uriAt < 0) return prefix;
        int directiveStart = head.lastIndexOf("<%@", uriAt);
        int directiveEnd = head.indexOf("%>", uriAt);
        if (directiveStart < 0 || directiveEnd < uriAt) return prefix;
        String directive = head.substring(directiveStart, directiveEnd);
        String declared = attributeValue(directive, "prefix");
        return declared.isEmpty() ? prefix : declared;
    }

    private static String attributeValue(String text, String attribute) {
        String needle = attribute + "=";
        int at = text.indexOf(needle);
        if (at < 0) return "";
        int valueStart = at + needle.length();
        if (valueStart >= text.length()) return "";
        char quote = text.charAt(valueStart);
        if (quote != '"' && quote != '\'') return "";
        int valueEnd = text.indexOf(quote, valueStart + 1);
        return valueEnd < 0 ? "" : text.substring(valueStart + 1, valueEnd);
    }

    private static String currentTagPrefix(String before) {
        int open = before.lastIndexOf('<');
        if (open < 0) return "";
        int colon = before.indexOf(':', open);
        if (colon < 0) return "";
        for (int i = open + 1; i < colon; i++) {
            char c = before.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') return "";
        }
        return before.substring(open + 1, colon);
    }

    private static boolean insideEl(String before) {
        int dollar = Math.max(before.lastIndexOf("${"), before.lastIndexOf("#{"));
        int close = before.lastIndexOf('}');
        return dollar >= 0 && dollar > close;
    }

    private static boolean insideTag(String before) {
        int open = before.lastIndexOf('<');
        int close = before.lastIndexOf('>');
        return open > close;
    }

    private static String commonTail(String tag) {
        if (tag.endsWith(":if") || tag.endsWith(":when")) return "test";
        if (tag.endsWith(":forEach")) return "items";
        if (tag.endsWith(":out")) return "value";
        if (tag.endsWith(":set")) return "var";
        if (tag.endsWith(":url")) return "value";
        if (tag.endsWith(":message")) return "key";
        return "";
    }

    private static final class DeclaredPrefixes {
        private final String corePrefix;
        private final String fmtPrefix;

        private DeclaredPrefixes(String corePrefix, String fmtPrefix) {
            this.corePrefix = corePrefix;
            this.fmtPrefix = fmtPrefix;
        }
    }
}
