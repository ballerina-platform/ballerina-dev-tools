package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.syntax.tree.*;

public class ParentStatementFindingVisitor extends NodeVisitor {
    private StatementNode parentStatement;
    private Node analyzingNode;

    public void analyze(Node node) {
        this.parentStatement = null;
        this.analyzingNode = node.parent();
        this.analyzingNode.accept(this);
        if (this.parentStatement == null) {
            this.analyze(this.analyzingNode);
        }
    }

    public StatementNode getParentStatement() {
        return parentStatement;
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        this.parentStatement = assignmentStatementNode;
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        this.parentStatement = variableDeclarationNode;
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        this.parentStatement = compoundAssignmentStatementNode;
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        this.parentStatement = expressionStatementNode;
    }
}
