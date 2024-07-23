package net.liopyu.kotlinscript.ast;

import net.liopyu.kotlinscript.util.Parser;
import net.liopyu.kotlinscript.util.Parsable;

import java.util.List;

public class FunctionCallNode extends ASTNode implements Parsable {
    private String functionName;
    private List<ASTNode> arguments;

    public FunctionCallNode() {}

    public FunctionCallNode(String functionName, List<ASTNode> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ASTNode> getArguments() {
        return arguments;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public ASTNode parse(Parser parser) {
        // This should be handled by the parser's `parseFunctionCall` method
        return this;
    }
}
