package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.Tokenizer;

public class ParseException extends RuntimeException {
    public Token token;

    public ParseException(Token token, String message) {
        super(message);
        this.token = token;
    }
    public ParseException(String message) {
        super(message);
    }
}