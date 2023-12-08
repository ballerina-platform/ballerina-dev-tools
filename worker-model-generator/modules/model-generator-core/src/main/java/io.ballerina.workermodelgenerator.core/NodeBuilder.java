package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AsyncSendActionNode;
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
import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.model.CanvasPosition;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.InputPort;
import io.ballerina.workermodelgenerator.core.model.OutputPort;
import io.ballerina.workermodelgenerator.core.model.WorkerNode;
import io.ballerina.workermodelgenerator.core.model.WorkerNodeJsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builder implementation for creating a {@link WorkerNode} instance.
 *
 * @since 2201.9.0
 */
class NodeBuilder extends NodeVisitor implements WorkerNodeJsonBuilder {

    private final SemanticModel semanticModel;

    // Json variables
    private String id;
    private TemplateKind templateKind;
    private CodeLocation codeLocation;
    private CanvasPosition canvasPosition;
    private final List<InputPort> inputPorts;
    private final List<OutputPort> outputPorts;

    // State variables
    private String toWorker;
    private String fromWorker;
    private TypeDescKind type;
    private String name;

    public NodeBuilder(SemanticModel semanticModel) {
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.semanticModel = semanticModel;
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
    public void visit(ReceiveActionNode receiveActionNode) {
        Node receiverWorker = receiveActionNode.receiveWorkers();
        if (receiverWorker.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
            this.fromWorker = ((SimpleNameReferenceNode) receiverWorker).name().text();
        }
    }

    @Override
    public void visit(AsyncSendActionNode asyncSendActionNode) {
        analyzeSendAction(asyncSendActionNode.peerWorker(), asyncSendActionNode.expression());
    }

    @Override
    public void visit(SyncSendActionNode syncSendActionNode) {
        analyzeSendAction(syncSendActionNode.peerWorker(), syncSendActionNode.expression());
    }

    private void analyzeSendAction(SimpleNameReferenceNode receiverNode, ExpressionNode expressionNode) {
        this.toWorker = receiverNode.name().text();
        Optional<TypeSymbol> typeSymbol = this.semanticModel.typeOf(expressionNode);
        this.type = typeSymbol.isPresent() ? typeSymbol.get().typeKind() : TypeDescKind.NONE;
        this.addOutputPort("id", this.type, this.toWorker);
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        // Find the name of the sender
        Optional<ExpressionNode> initializer = variableDeclarationNode.initializer();
        if (initializer.isEmpty()) {
            return;
        }
        initializer.get().accept(this);

        // Find the parameter name
        TypedBindingPatternNode typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
        typedBindingPatternNode.bindingPattern().accept(this);

        // Find the parameter type
        Optional<Symbol> symbol = this.semanticModel.symbol(typedBindingPatternNode.typeDescriptor());
        this.type = (symbol.isPresent() && symbol.get() instanceof TypeSymbol typeSymbol) ? typeSymbol.typeKind() :
                TypeDescKind.NONE;

        this.addInputPort("id", this.type, this.name, this.fromWorker);
    }

    @Override
    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {
        this.name = captureBindingPatternNode.variableName().text();
    }

    @Override
    public void setName(String id) {
        this.id = id;
    }

    @Override
    public void setTemplateKind(TemplateKind templateKind) {
        this.templateKind = templateKind;
    }

    @Override
    public void addInputPort(String id, TypeDescKind type, String name, String sender) {
        this.inputPorts.add(new InputPort(id, type, name, sender));
    }

    @Override
    public void addOutputPort(String id, TypeDescKind type, String receiver) {
        this.outputPorts.add(new OutputPort(id, type, receiver));
    }

    @Override
    public void setCodeLocation(LinePosition start, LinePosition end) {
        this.codeLocation = new CodeLocation(start, end);
    }

    @Override
    public void setCanvasPosition(int x, int y) {
        this.canvasPosition = new CanvasPosition(x, y);
    }

    @Override
    public WorkerNode build() {
        return new WorkerNode(id, templateKind, codeLocation, canvasPosition, inputPorts, outputPorts);
    }
}
