package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.token.Token;
import net.liopyu.kotlinscript.token.TokenType;
import net.liopyu.kotlinscript.util.ContextUtils;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

import java.util.ArrayList;
import java.util.List;

public class FunctionDeclarationNode extends ASTNode implements Parsable {
    private String name;
    private List<ContextUtils.Parameter> parameters;
    private BlockNode body;

    public FunctionDeclarationNode() {}

    public FunctionDeclarationNode(String name, List<ContextUtils.Parameter> parameters, BlockNode body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    public String getName() {
        return name;
    }

    public List<ContextUtils.Parameter> getParameters() {
        return parameters;
    }

    public BlockNode getBody() {
        return body;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        parser.consume(TokenType.KEYWORD); // consume 'fun'
        Token functionName = parser.consume(TokenType.IDENTIFIER);
        parser.consume(TokenType.LEFT_PAREN); // consume '('

        List<ContextUtils.Parameter> parameters = new ArrayList<>();
        while (parser.currentToken().getType() != TokenType.RIGHT_PAREN) {
            Token paramName = parser.consume(TokenType.IDENTIFIER);
            parser.consume(TokenType.COLON); // consume ':'
            Token paramType = parser.consume(TokenType.IDENTIFIER); // Assume types are also identifiers

            parameters.add(new ContextUtils.Parameter(paramName.getValue(), paramType.getValue()));

            if (parser.currentToken().getType() == TokenType.COMMA) {
                parser.consume(TokenType.COMMA); // consume ','
            }
        }

        parser.consume(TokenType.RIGHT_PAREN); // consume ')'
        BlockNode body = (BlockNode) new BlockNode().parse(parser);

        this.name = functionName.getValue();
        this.parameters = parameters;
        this.body = body;
        return this;
    }


}