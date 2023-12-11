package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AsyncSendActionNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.BracedExpressionNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ElseBlockNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ReceiveActionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyncSendActionNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.model.CanvasPosition;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.InputPort;
import io.ballerina.workermodelgenerator.core.model.OutputPort;
import io.ballerina.workermodelgenerator.core.model.SwitchCase;
import io.ballerina.workermodelgenerator.core.model.SwitchDefaultCase;
import io.ballerina.workermodelgenerator.core.model.SwitchProperties;
import io.ballerina.workermodelgenerator.core.model.WorkerNode;
import io.ballerina.workermodelgenerator.core.model.WorkerNodeJsonBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private String templateId;
    private CodeLocation codeLocation;
    private CanvasPosition canvasPosition;
    private final List<InputPort> inputPorts;
    private final List<OutputPort> outputPorts;
    private String codeBlock;

    // State variables
    private String toWorker;
    private String fromWorker;
    private String type;
    private String name;
    private int portId;
    private boolean capturedFromWorker;
    private boolean hasProcessed;
    private SwitchState switchState;
    private String expresison;
    private final Map<String, List<String>> expressionToNodesMapper;
    private final List<String> defaultSwitchCaseNodes;
    private SwitchProperties switchProperties;

    public NodeBuilder(SemanticModel semanticModel) {
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.portId = 0;
        this.expressionToNodesMapper = new LinkedHashMap<>();
        this.defaultSwitchCaseNodes = new ArrayList<>();
        this.switchState = SwitchState.OFF;
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
        this.capturedFromWorker = true;
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
        this.type = typeSymbol.isPresent() ? typeSymbol.get().signature() : TypeDescKind.NONE.toString();

        // Capture the name if the expression is a variable
        String name = expressionNode.kind() == SyntaxKind.SIMPLE_NAME_REFERENCE ?
                ((SimpleNameReferenceNode) expressionNode).name().text() : null;

        String portIdStr = String.valueOf(++this.portId);
        this.addOutputPort(portIdStr, this.type, name, this.toWorker);
        if (this.switchState == SwitchState.CONDITIONAL) {
            addSwitchCase(this.expresison, portIdStr);
        } else if (this.switchState == SwitchState.DEFAULT) {
            addDefaultSwitchCase(portIdStr);
        }
        this.hasProcessed = true;
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
        this.type = (symbol.isPresent() && symbol.get() instanceof TypeSymbol typeSymbol) ? typeSymbol.signature() :
                TypeDescKind.NONE.toString();

        this.addInputPort(String.valueOf(++this.portId), this.type, this.name, this.fromWorker);
        this.capturedFromWorker = false;
        this.hasProcessed = true;
    }

    @Override
    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {
        this.name = captureBindingPatternNode.variableName().text();
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        this.switchState = SwitchState.CONDITIONAL;
        ifElseStatementNode.condition().accept(this);
        ifElseStatementNode.ifBody().accept(this);
        ifElseStatementNode.elseBody().ifPresent(elseBody -> elseBody.accept(this));
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        blockStatementNode.statements().forEach(statement -> statement.accept(this));
    }

    @Override
    public void visit(ElseBlockNode elseBlockNode) {
        StatementNode elseBody = elseBlockNode.elseBody();
        this.switchState =
                elseBody.kind() == SyntaxKind.BLOCK_STATEMENT ? SwitchState.DEFAULT : SwitchState.CONDITIONAL;
        elseBody.accept(this);
    }

    @Override
    public void visit(BracedExpressionNode bracedExpressionNode) {
        this.expresison = bracedExpressionNode.expression().toSourceCode();
    }

    public boolean hasProcessed() {
        return this.hasProcessed;
    }

    public void resetProcessFlag() {
        this.hasProcessed = false;
    }

    @Override
    public void setName(String id) {
        this.id = id;
    }

    @Override
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    @Override
    public void addInputPort(String id, String type, String name, String sender) {
        this.inputPorts.add(new InputPort(id, type, name, sender));
    }

    @Override
    public void addOutputPort(String id, String type, String name, String receiver) {
        this.outputPorts.add(new OutputPort(id, type, name, receiver));
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
    public void setCodeBlock(String codeBlock) {
        this.codeBlock = codeBlock;
    }

    @Override
    public void addSwitchCase(String expression, String node) {
        List<String> currentNodes = this.expressionToNodesMapper.get(expression);
        if (currentNodes == null) {
            List<String> nodes = new ArrayList<>();
            nodes.add(node);
            this.expressionToNodesMapper.put(expression, nodes);
            return;
        }
        currentNodes.add(node);
    }

    @Override
    public void addDefaultSwitchCase(String node) {
        this.defaultSwitchCaseNodes.add(node);
    }

    @Override
    public void buildSwitchCaseProperties() {
        if (this.defaultSwitchCaseNodes.isEmpty()) {
            return;
        }
        List<SwitchCase> switchCases = new ArrayList<>();
        for (Map.Entry<String, List<String>> switchCaseEntry : this.expressionToNodesMapper.entrySet()) {
            switchCases.add(new SwitchCase(switchCaseEntry.getKey(), switchCaseEntry.getValue()));
        }
        this.switchProperties = new SwitchProperties(switchCases, new SwitchDefaultCase(this.defaultSwitchCaseNodes));
    }

    @Override
    public WorkerNode build() {
        return new WorkerNode(id, templateId, codeLocation, canvasPosition, inputPorts, outputPorts, codeBlock,
                this.switchProperties);
    }

    private enum SwitchState {
        OFF,
        CONDITIONAL,
        DEFAULT
    }
}
