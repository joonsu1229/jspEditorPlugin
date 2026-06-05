package com.github.codex.jsplite;

import com.intellij.lang.Language;

public final class JspLiteLanguage extends Language {
    public static final JspLiteLanguage INSTANCE = new JspLiteLanguage();

    private JspLiteLanguage() {
        super("JSP Lite");
    }
}
