package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;

/**
 * Visitor to identify the return statements.
 *
 * @since 2201.8.0
 */
public class ReturnStatementVisitor extends NodeVisitor {
    private ReturnStatementNode returnStatement;

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        this.returnStatement = returnStatementNode;
    }

    public ReturnStatementNode getReturnStatement() {
        return returnStatement;
    }
}
