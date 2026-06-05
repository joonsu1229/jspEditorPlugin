package com.github.codex.jsplite.lexer;

import com.github.codex.jsplite.JspLiteLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class JspLiteTokenType extends IElementType {
    public JspLiteTokenType(@NotNull @NonNls String debugName) {
        super(debugName, JspLiteLanguage.INSTANCE);
    }
}
