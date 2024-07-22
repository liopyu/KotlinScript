package net.liopyu.kotlinscript.ast;

public class PrintNode extends ASTNode {
    private String variableName;

    public PrintNode(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public void accept(ASTVisitor visitor) {
        visitor.visit(this);
    }
}
