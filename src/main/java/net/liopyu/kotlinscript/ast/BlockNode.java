package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.ast.astinterface.ASTVisitor;
import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.VariableNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends ASTNode {
    private List<ASTNode> statements;

    public BlockNode() {
        this.statements = new ArrayList<>();
    }

    public void addStatement(ASTNode statement) {
        statements.add(statement);
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
    public BlockNode parse(Parser parser) {
        parser.currentTokenIndex++; // Skip '{'
        while (parser.currentTokenIndex < parser.tokens.size() && !parser.tokens.get(parser.currentTokenIndex).getValue().equals("}")) {
            ASTNode statement = parser.parseStatement();
            if (statement != null) {
                this.addStatement(statement);
            } else {
                parser.currentTokenIndex++;
            }
        }
        if (parser.currentTokenIndex < parser.tokens.size() && parser.tokens.get(parser.currentTokenIndex).getValue().equals("}")) {
            parser.currentTokenIndex++; // Skip '}'
        } else {
            throw new VariableNotFoundException("Expected '}' but found end of file or another token");
        }
        return this;
    }


    @Override
    public void append(AstStringBuilder builder) {

    }
}