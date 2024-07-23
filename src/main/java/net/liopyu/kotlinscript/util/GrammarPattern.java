package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;

import java.util.List;
import java.util.function.Function;

public class GrammarPattern {
    private List<Object> pattern;
    private Function<ParserContext, ASTNode> handler;

    public GrammarPattern(List<Object> pattern, Function<ParserContext, ASTNode> handler) {
        this.pattern = pattern;
        this.handler = handler;
    }

    public List<Object> getPattern() {
        return pattern;
    }

    public Function<ParserContext, ASTNode> getHandler() {
        return handler;
    }
}