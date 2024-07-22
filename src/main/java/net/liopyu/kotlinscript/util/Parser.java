package net.liopyu.kotlinscript.util;

import net.liopyu.kotlinscript.ast.*;
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
            ASTNode node = parseStatement();
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private ASTNode parseStatement() {
        if (currentTokenIndex >= tokens.size()) {
            return null;
        }
        Token token = tokens.get(currentTokenIndex);
        switch (token.getType()) {
            case KEYWORD:
                if (token.getValue().equals("var")) {
                    return parseVariableDeclaration();
                } else if (token.getValue().equals("print")) {
                    return parsePrintStatement();
                }
                break;
            case PARENTHESIS:
                if (token.getValue().equals("{")) {
                    return parseBlock();
                }
                break;
            case COMMENT:
                return parseComment();
            default:
                currentTokenIndex++;
                break;
        }
        return null;
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

    private BlockNode parseBlock() {
        currentTokenIndex++; // Skip '{'
        BlockNode block = new BlockNode();
        while (currentTokenIndex < tokens.size() && !tokens.get(currentTokenIndex).getValue().equals("}")) {
            ASTNode statement = parseStatement();
            if (statement != null) {
                block.addStatement(statement);
            }
        }
        currentTokenIndex++; // Skip '}'
        return block;
    }

    private CommentNode parseComment() {
        Token commentToken = tokens.get(currentTokenIndex);
        currentTokenIndex++; // Skip the comment
        return new CommentNode(commentToken.getValue());
    }

    private Object parseValue(Token valueToken) {
        switch (valueToken.getType()) {
            case NUMBER:
                return parseNumber(valueToken.getValue());
            case STRING:
                return valueToken.getValue(); // Keep the quotes
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