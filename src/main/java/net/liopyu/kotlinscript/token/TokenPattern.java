package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.token.TokenType;

import java.util.regex.Pattern;

public class TokenPattern {
    private final Pattern pattern;
    private final TokenType type;

    public TokenPattern(String regex, TokenType type) {
        this.pattern = Pattern.compile(regex);
        this.type = type;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public TokenType getType() {
        return type;
    }
}
