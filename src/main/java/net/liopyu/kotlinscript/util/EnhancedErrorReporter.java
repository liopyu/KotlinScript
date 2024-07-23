package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.token.Tokenizer;

public class EnhancedErrorReporter {
    public static void reportError(String message, Tokenizer.Token token) {
        System.err.println("[Error] " + message + " at " + token.value + " (type: " + token.type + ")");
    }

    public static void reportError(String message, ASTNode node) {
        System.err.println("[Error] " + message + " at node " + node.getClass().getSimpleName());
    }
}
