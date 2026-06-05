package com.github.codex.jsplite;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public final class JspLiteFileType extends LanguageFileType {
    public static final JspLiteFileType INSTANCE = new JspLiteFileType();

    private JspLiteFileType() {
        super(JspLiteLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "JSP Lite";
    }

    @Override
    public @NotNull String getDescription() {
        return "JSP Lite file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "jsp";
    }

    @Override
    public @Nullable Icon getIcon() {
        return JspLiteIcons.FILE;
    }
}
