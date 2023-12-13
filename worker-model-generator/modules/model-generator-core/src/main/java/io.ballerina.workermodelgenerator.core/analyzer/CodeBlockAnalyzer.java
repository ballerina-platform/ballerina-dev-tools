package io.ballerina.workermodelgenerator.core.analyzer;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ReceiveActionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.NodeBuilder;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.properties.BalExpression;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

/**
 * Syntax tree analyzer to obtain information from a code block node.
 *
 * @since 2201.9.0
 */
public class CodeBlockAnalyzer extends Analyzer {

    private boolean hasProcessed;
    private BalExpression balExpression;

    protected CodeBlockAnalyzer(NodeBuilder nodeBuilder,
                                SemanticModel semanticModel) {
        super(nodeBuilder, semanticModel);
    }

    @Override
    protected void analyzeSendAction(SimpleNameReferenceNode receiverNode, ExpressionNode expressionNode) {
        super.analyzeSendAction(receiverNode, expressionNode);
        this.hasProcessed = true;
    }

    @Override
    public void visit(ReceiveActionNode receiveActionNode) {
        super.visit(receiveActionNode);
        this.hasProcessed = true;
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        StringBuilder codeBlock = new StringBuilder();
        boolean isStart = true;
        LinePosition start = null;
        LinePosition end = null;
        for (StatementNode statement : blockStatementNode.statements()) {
            statement.accept(this);
            if (!this.hasProcessed) {
                if (isStart) {
                    start = statement.lineRange().startLine();
                    isStart = false;
                }
                end = statement.lineRange().endLine();
                codeBlock.append(statement.toSourceCode());
            }
            this.hasProcessed = false;
        }
        this.balExpression = new BalExpression(codeBlock.toString(), new CodeLocation(start, end));
    }

    @Override
    public NodeProperties buildProperties() {
        NodeProperties.NodePropertiesBuilder nodePropertiesBuilder = new NodeProperties.NodePropertiesBuilder();
        return nodePropertiesBuilder.setCodeBlock(this.balExpression).build();
    }
}
