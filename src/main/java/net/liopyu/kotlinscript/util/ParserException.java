package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.token.Token;

public class ParserException extends RuntimeException {
    public ParserException(Token token, String message) {
        super("Parser Exception at token: " + token + " - " +  message);
    }
}