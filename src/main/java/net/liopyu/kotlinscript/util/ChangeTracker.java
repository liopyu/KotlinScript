package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.token.TokenList;

public class ChangeTracker {
    private String script;
    private TokenList tokenList;

    public ChangeTracker(String script) {
        this.script = script;
        this.tokenList = new TokenList();
        // Initial tokenization
        this.tokenList.updateTokens(0, script.length(), script);
    }

    public void applyChange(int start, int end, String replacement) {
        // Update script string
        script = script.substring(0, start) + replacement + script.substring(end);
        // Update tokens accordingly
        tokenList.updateTokens(start, start + replacement.length(), replacement);
    }
}
