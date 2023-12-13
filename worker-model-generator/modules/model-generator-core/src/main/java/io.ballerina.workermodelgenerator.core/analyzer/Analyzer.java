package io.ballerina.workermodelgenerator.core.analyzer;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AsyncSendActionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ReceiveActionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyncSendActionNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.workermodelgenerator.core.Constants;
import io.ballerina.workermodelgenerator.core.NodeBuilder;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

import java.util.Optional;

/**
 * The default syntax tree analyzer to obtain information from a worker node. The implementation primarily concerns on
 * analyzing the send and receive actions of a worker node.
 *
 * @since 2201.9.0
 */
public class Analyzer extends NodeVisitor {

    private final SemanticModel semanticModel;
    private final NodeBuilder nodeBuilder;
    protected String toWorker;
    protected String fromWorker;
    private String name;
    protected int portId;
    protected boolean capturedFromWorker;

    protected Analyzer(NodeBuilder nodeBuilder, SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.nodeBuilder = nodeBuilder;
        this.portId = 0;
    }

    /**
     * Build the properties of the node.
     *
     * @return {@link NodeProperties} of the node
     */
    public NodeProperties buildProperties() {
        return null;
    }

    /**
     * Get the analyzer for the given template id.
     *
     * @param templateId    Template id of the node
     * @param nodeBuilder   Node builder
     * @param semanticModel Semantic model
     * @return {@link Analyzer} for the given template id
     */
    public static Analyzer getAnalyzer(String templateId, NodeBuilder nodeBuilder,
                                       SemanticModel semanticModel) {
        return switch (templateId) {
            case Constants.SWITCH_NODE -> new SwitchAnalyzer(nodeBuilder, semanticModel);
            case Constants.BLOCK_NODE -> new CodeBlockAnalyzer(nodeBuilder, semanticModel);
            // TODO: Handle invalid template id
            default -> new Analyzer(nodeBuilder, semanticModel);
        };
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        blockStatementNode.statements().forEach(statement -> statement.accept(this));
    }

    @Override
    public void visit(AsyncSendActionNode asyncSendActionNode) {
        analyzeSendAction(asyncSendActionNode.peerWorker(), asyncSendActionNode.expression());
    }

    @Override
    public void visit(SyncSendActionNode syncSendActionNode) {
        analyzeSendAction(syncSendActionNode.peerWorker(), syncSendActionNode.expression());
    }

    protected void analyzeSendAction(SimpleNameReferenceNode receiverNode, ExpressionNode expressionNode) {
        this.toWorker = receiverNode.name().text();
        Optional<TypeSymbol> typeSymbol = this.semanticModel.typeOf(expressionNode);
        String type = typeSymbol.isPresent() ? typeSymbol.get().signature() : TypeDescKind.NONE.toString();

        // Capture the name if the expression is a variable
        String name = expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE ?
                ((SimpleNameReferenceNode) expressionNode).name().text() : null;

        this.portId++;
        this.nodeBuilder.addOutputPort(getPortId(), type, name, this.toWorker);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        // Find the name of the sender
        Optional<ExpressionNode> initializer = variableDeclarationNode.initializer();
        if (initializer.isEmpty()) {
            return;
        }
        initializer.get().accept(this);

        if (!this.capturedFromWorker) {
            return;
        }

        // Find the parameter name
        TypedBindingPatternNode typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
        typedBindingPatternNode.bindingPattern().accept(this);

        // Find the parameter type
        Optional<Symbol> symbol = this.semanticModel.symbol(typedBindingPatternNode.typeDescriptor());
        String type = (symbol.isPresent() && symbol.get() instanceof TypeSymbol typeSymbol) ? typeSymbol.signature() :
                TypeDescKind.NONE.toString();

        this.portId++;
        this.nodeBuilder.addInputPort(getPortId(), type, this.name, this.fromWorker);
        this.capturedFromWorker = false;
    }

    @Override
    public void visit(ReceiveActionNode receiveActionNode) {
        Node receiverWorker = receiveActionNode.receiveWorkers();
        if (receiverWorker.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            this.fromWorker = ((SimpleNameReferenceNode) receiverWorker).name().text();
        }
        this.capturedFromWorker = true;
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        ExpressionNode expression = expressionStatementNode.expression();
        expression.accept(this);
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        checkExpressionNode.expression().accept(this);
    }

    @Override
    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {
        this.name = captureBindingPatternNode.variableName().text();
    }

    protected final String getPortId() {
        return String.valueOf(this.portId);
    }
}
