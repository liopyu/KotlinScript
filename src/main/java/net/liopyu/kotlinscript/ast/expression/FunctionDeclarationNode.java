package net.liopyu.kotlinscript.ast.expression;

import net.liopyu.kotlinscript.ast.ASTNode;
import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.ast.AstStringBuilder;
import net.liopyu.kotlinscript.ast.BlockNode;
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
        parser.consume(TokenType.FUN); // consume 'fun'
        Token functionName = parser.consume(TokenType.IDENTIFIER);
        parser.consume(TokenType.LPAREN); // consume '('

        List<ContextUtils.Parameter> parameters = new ArrayList<>();
        while (parser.currentToken().getType() != TokenType.RPAREN) {
            Token paramName = parser.consume(TokenType.IDENTIFIER);
            parser.consume(TokenType.COLON); // consume ':'
            Token paramType = parser.consume(TokenType.IDENTIFIER); // Assume types are also identifiers

            parameters.add(new ContextUtils.Parameter(paramName.getValue(), paramType.getValue()));

            if (parser.currentToken().getType() == TokenType.COMMARIGHT) {
                parser.consume(TokenType.COMMARIGHT); // consume ','
            }
        }

        parser.consume(TokenType.RPAREN); // consume ')'
        BlockNode body = (BlockNode) new BlockNode().parse(parser);

        this.name = functionName.getValue();
        this.parameters = parameters;
        this.body = body;
        return this;
    }


    @Override
    public void append(AstStringBuilder builder) {

    }
}