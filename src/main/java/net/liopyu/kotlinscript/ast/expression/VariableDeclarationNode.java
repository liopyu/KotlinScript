package net.liopyu.kotlinscript.ast.expression;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.AstStringBuilder;
import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.util.Parser;

public class VariableDeclarationNode extends ASTNode {
    private String name;
    private Object value;

    public VariableDeclarationNode(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
    public VariableDeclarationNode parse(Parser parser) {
        parser.currentTokenIndex++; // Skip 'var'
        Token identifier = parser.tokens.get(parser.currentTokenIndex);
        parser.currentTokenIndex++; // Skip identifier
        parser.currentTokenIndex++; // Skip '='
        Token valueToken = parser.tokens.get(parser.currentTokenIndex);
        parser.currentTokenIndex++; // Skip value token
        Object value = parseValue(valueToken);
        this.setName(identifier.getValue());
        this.setValue(value);
        return this;
    }
    public Object parseValue(Token valueToken) {
        switch (valueToken.getType()) {
            case FLOATING:
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

    @Override
    public void append(AstStringBuilder builder) {

    }
}
