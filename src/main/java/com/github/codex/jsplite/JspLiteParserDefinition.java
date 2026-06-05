package com.github.codex.jsplite;

import com.github.codex.jsplite.lexer.JspLiteLexer;
import com.github.codex.jsplite.lexer.JspLiteTokenTypes;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.extapi.psi.PsiFileBase;
import org.jetbrains.annotations.NotNull;

public final class JspLiteParserDefinition implements ParserDefinition {
    private static final IFileElementType FILE = new IFileElementType(JspLiteLanguage.INSTANCE);
    private static final TokenSet WHITE_SPACES = TokenSet.create(JspLiteTokenTypes.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(
            JspLiteTokenTypes.JSP_COMMENT,
            JspLiteTokenTypes.HTML_COMMENT,
            JspLiteTokenTypes.CODE_COMMENT
    );
    private static final TokenSet STRINGS = TokenSet.create(
            JspLiteTokenTypes.HTML_STRING,
            JspLiteTokenTypes.STRING
    );

    @Override
    public @NotNull JspLiteLexer createLexer(Project project) {
        return new JspLiteLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return (root, builder) -> {
            PsiBuilder.Marker marker = builder.mark();
            while (!builder.eof()) {
                builder.advanceLexer();
            }
            marker.done(root);
            return builder.getTreeBuilt();
        };
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new PsiFileBase(viewProvider, JspLiteLanguage.INSTANCE) {
            @Override
            public @NotNull JspLiteFileType getFileType() {
                return JspLiteFileType.INSTANCE;
            }

            @Override
            public String toString() {
                return "JSP Lite file";
            }
        };
    }

    @Override
    public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
