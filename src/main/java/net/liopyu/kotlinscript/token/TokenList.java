package net.liopyu.kotlinscript.token;

import java.util.ArrayList;

public class TokenList {
    private ArrayList<Token> tokens;

    public TokenList() {
        this.tokens = new ArrayList<>();
    }

    // Part of TokenList class
    public void updateTokens(int start, int end, String newText) {
        // Remove tokens within 'start' and 'end'
        // This is a simplification
        tokens.removeIf(token -> token.position >= start && token.position < end);

        // Re-tokenize 'newText'
        ArrayList<Token> newTokens = Tokenizer.tokenize(newText);
        newTokens.forEach(token -> token.position += start); // Adjust positions
        tokens.addAll(newTokens);

        // Correct positions of subsequent tokens
        int delta = newText.length() - (end - start);
        for (Token token : tokens) {
            if (token.position > end) {
                token.position += delta;
            }
        }
    }

}