package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.PrintNode;
import net.liopyu.kotlinscript.ast.VariableDeclarationNode;
import net.liopyu.kotlinscript.token.Token;

import java.util.ArrayList;

public class Parser {
    private ArrayList<Token> tokens;
    private int currentTokenIndex = 0;

    public Parser(ArrayList<Token> tokens) {
        this.tokens = tokens;
    }

    public ArrayList<ASTNode> parse() {
        ArrayList<ASTNode> nodes = new ArrayList<>();
        while (currentTokenIndex < tokens.size()) {
            Token token = tokens.get(currentTokenIndex);
            switch (token.getType()) {
                case KEYWORD:
                    if (token.getValue().equals("var")) {
                        nodes.add(parseVariableDeclaration());
                    } else if (token.getValue().equals("print")) {
                        nodes.add(parsePrintStatement());
                    }
                    break;
                default:
                    currentTokenIndex++;
                    break;
            }
        }
        return nodes;
    }

    private VariableDeclarationNode parseVariableDeclaration() {
        currentTokenIndex++; // Skip 'var'
        Token identifier = tokens.get(currentTokenIndex);
        currentTokenIndex++; // Skip identifier
        currentTokenIndex++; // Skip '='
        Token valueToken = tokens.get(currentTokenIndex);
        currentTokenIndex++; // Skip value token

        Object value = parseValue(valueToken);
        return new VariableDeclarationNode(identifier.getValue(), value);
    }

    private PrintNode parsePrintStatement() {
        currentTokenIndex++; // Skip 'print'
        currentTokenIndex++; // Skip '('
        Token identifier = tokens.get(currentTokenIndex);
        currentTokenIndex++; // Skip identifier
        currentTokenIndex++; // Skip ')'

        return new PrintNode(identifier.getValue());
    }

    private Object parseValue(Token valueToken) {
        switch (valueToken.getType()) {
            case NUMBER:
                return parseNumber(valueToken.getValue());
            case STRING:
                return valueToken.getValue().replace("\"", "");
            case IDENTIFIER:
                if (valueToken.getValue().equals("true") || valueToken.getValue().equals("false")) {
                    return Boolean.parseBoolean(valueToken.getValue());
                }
                return valueToken.getValue();
            default:
                return null;
        }
    }

    private Object parseNumber(String value) {
        if (value.contains(".")) {
            return Double.parseDouble(value);
        } else {
            return Integer.parseInt(value);
        }
    }
}
