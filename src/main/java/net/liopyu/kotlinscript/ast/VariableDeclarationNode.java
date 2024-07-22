package net.liopyu.kotlinscript.ast;

public class VariableDeclarationNode extends ASTNode {
    private String name;
    private Object value;

    public VariableDeclarationNode(String name, Object value) {
        this.name = name;
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
}
