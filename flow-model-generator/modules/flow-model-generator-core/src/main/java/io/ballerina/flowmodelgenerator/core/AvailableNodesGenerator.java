/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DatabaseManager;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Generates available nodes for a given position in the diagram.
 *
 * @since 2.0.0
 */
public class AvailableNodesGenerator {

    private final Category.Builder rootBuilder;
    private final SemanticModel semanticModel;
    private final Document document;
    private final Gson gson;
    private static final String BALLERINA_ORG = "ballerina";
    private static final String HTTP_MODULE = "http";
    private static final List<String> HTTP_REMOTE_METHOD_SKIP_LIST = List.of("get", "put", "post", "head",
            "delete", "patch", "options");
    private static final String WSO2 = "wso2";
    private static final String AI_AGENT = "ai.agent";
    private static final String AI_VERSION = "1.0.0";

    private static final List<String> agents = List.of("FunctionCallAgent", "ReActAgent", "Agent");

    public AvailableNodesGenerator(SemanticModel semanticModel, Document document) {
        this.rootBuilder = new Category.Builder(null).name(Category.Name.ROOT);
        this.gson = new Gson();
        this.semanticModel = semanticModel;
        this.document = document;
    }

    public JsonArray getAvailableNodes(LinePosition position) {
        List<Item> connectionItems = new ArrayList<>();
        semanticModel.visibleSymbols(document, position).stream()
                .flatMap(symbol -> getConnection(symbol).stream())
                .sorted(Comparator.comparing(category -> category.metadata().label()))
                .forEach(connectionItems::add);
        this.rootBuilder.stepIn(Category.Name.CONNECTIONS).items(connectionItems).stepOut();

        List<Item> items = new ArrayList<>();
        items.addAll(getAvailableFlowNodes(position));
        items.addAll(LocalIndexCentral.getInstance().getFunctions());
        genAvailableAgents(items);
        return gson.toJsonTree(items).getAsJsonArray();
    }

    private List<Item> getAvailableFlowNodes(LinePosition cursorPosition) {
        int txtPos = this.document.textDocument().textPositionFrom(cursorPosition);
        TextRange range = TextRange.from(txtPos, 0);
        NonTerminalNode nonTerminalNode = ((ModulePartNode) document.syntaxTree().rootNode()).findNode(range);

        while (nonTerminalNode != null) {
            SyntaxKind kind = nonTerminalNode.kind();
            switch (kind) {
                case WHILE_STATEMENT, FOREACH_STATEMENT -> {
                    setAvailableNodesForIteratingBlock(nonTerminalNode, this.semanticModel);
                    return this.rootBuilder.build().items();
                }
                case IF_ELSE_STATEMENT, LOCK_STATEMENT, TRANSACTION_STATEMENT, MATCH_STATEMENT, DO_STATEMENT,
                     ON_FAIL_CLAUSE -> {
                    setAvailableDefaultNodes(nonTerminalNode, semanticModel);
                    return this.rootBuilder.build().items();
                }
                default -> nonTerminalNode = nonTerminalNode.parent();
            }
        }
        setDefaultNodes();
        return this.rootBuilder.build().items();
    }

    private void genAvailableAgents(List<Item> items) {
        List<Symbol> symbols = semanticModel.moduleSymbols();
        List<Item> agentItems = new ArrayList<>();
        for (Symbol symbol : symbols) {
            if (symbol.kind() != SymbolKind.VARIABLE) {
                continue;
            }
            VariableSymbol variableSymbol = (VariableSymbol) symbol;
            String typeName = variableSymbol.typeDescriptor().getName().orElse("");
            if (agents.contains(typeName)) {
                Metadata metadata = new Metadata.Builder<>(null)
                        .label(variableSymbol.getName().orElse(""))
                        .build();
                FunctionData functionResult = new FunctionData(-1, "run", "Run agent", "error?", AI_AGENT, WSO2,
                        AI_VERSION, "",
                        FunctionData.Kind.FUNCTION, true, false);
                NodeBuilder methodCallBuilder = NodeBuilder.getNodeFromKind(NodeKind.AGENT_CALL);
                methodCallBuilder
                        .metadata()
                            .label(functionResult.name())
                            .icon(CommonUtils.generateIcon(WSO2, AI_AGENT, AI_VERSION))
                            .description(functionResult.description())
                            .stepOut()
                        .codedata()
                            .node(NodeKind.AGENT_CALL)
                            .org(WSO2)
                            .module(AI_AGENT)
                            .version(AI_VERSION)
                            .symbol(functionResult.name())
                            .id(functionResult.functionId());
                agentItems.add(new Category(metadata, List.of(methodCallBuilder.buildAvailableNode())));
            }
        }

        if (!agentItems.isEmpty()) {
            items.add(this.rootBuilder.stepIn(Category.Name.AGENTS).items(agentItems).build());
        }
    }

    private void setAvailableDefaultNodes(NonTerminalNode node, SemanticModel semanticModel) {
        setDefaultNodes();
        setStopNode(node);
    }

    private void setAvailableNodesForIteratingBlock(NonTerminalNode node, SemanticModel semanticModel) {
        setDefaultNodes();
        setStopNode(node);
        this.rootBuilder.stepIn(Category.Name.CONTROL)
                .node(NodeKind.BREAK)
                .node(NodeKind.CONTINUE)
                .stepOut();
    }

    private void setDefaultNodes() {
        AvailableNode function = new AvailableNode(
                new Metadata.Builder<>(null)
                        .label("Function Call")
                        .description("Both project and utility functions")
                        .build(),
                new Codedata.Builder<>(null)
                        .node(NodeKind.FUNCTION)
                        .build(),
                true
        );

        this.rootBuilder.stepIn(Category.Name.STATEMENT)
                .node(NodeKind.VARIABLE)
                .node(NodeKind.ASSIGN)
                .node(function)
                .node(NodeKind.DATA_MAPPER_CALL);

        this.rootBuilder.stepIn(Category.Name.CONTROL)
                .node(NodeKind.IF)
                .node(NodeKind.MATCH)
                .node(NodeKind.WHILE)
                .node(NodeKind.FOREACH)
                .node(NodeKind.RETURN);

        this.rootBuilder
                .stepIn(Category.Name.ERROR_HANDLING)
                    .node(NodeKind.ERROR_HANDLER)
                    .node(NodeKind.FAIL)
                    .node(NodeKind.PANIC)
                    .stepOut()
                .stepIn(Category.Name.CONCURRENCY)
                    .node(NodeKind.FORK)
                    .node(NodeKind.PARALLEL_FLOW)
                    .node(NodeKind.WAIT)
                    .node(NodeKind.LOCK)
                    .node(NodeKind.START)
                    .node(NodeKind.TRANSACTION)
                    .node(NodeKind.COMMIT)
                    .node(NodeKind.ROLLBACK)
                    .node(NodeKind.RETRY)
                    .stepOut();
    }

    private void setStopNode(NonTerminalNode node) {
        Node parent = node;
        while (parent != null) {
            if (isStopNodeAvailable(parent)) {
                this.rootBuilder.stepIn(Category.Name.CONTROL)
                        .node(NodeKind.STOP)
                        .stepOut();
            }
            parent = parent.parent();
        }
    }

    private boolean isStopNodeAvailable(Node node) {
        if (node.kind() != SyntaxKind.FUNCTION_DEFINITION &&
                node.kind() != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION &&
                node.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
            return false;
        }
        Optional<Symbol> symbol = this.semanticModel.symbol(node);
        if (symbol.isEmpty()) {
            return false;
        }
        Optional<TypeSymbol> typeSymbol = ((FunctionSymbol) symbol.get()).typeDescriptor().returnTypeDescriptor();
        return typeSymbol.isEmpty() || typeSymbol.get().subtypeOf(semanticModel.types().NIL);
    }

    private Optional<Category> getConnection(Symbol symbol) {
        try {
            TypeReferenceTypeSymbol typeDescriptorSymbol =
                    (TypeReferenceTypeSymbol) ((VariableSymbol) symbol).typeDescriptor();
            if (typeDescriptorSymbol.typeDescriptor().kind() != SymbolKind.CLASS ||
                    !((ClassSymbol) typeDescriptorSymbol.typeDescriptor()).qualifiers().contains(Qualifier.CLIENT)) {
                return Optional.empty();
            }

            ModuleSymbol moduleSymbol = typeDescriptorSymbol.typeDescriptor().getModule().orElseThrow();
            List<Item> connections = fetchConnections(moduleSymbol.id(), symbol.getName().orElse(""));

            Metadata metadata = new Metadata.Builder<>(null)
                    .label(symbol.getName().orElseThrow())
                    .build();
            return Optional.of(new Category(metadata, connections));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static List<Item> fetchConnections(ModuleID moduleId, String parentSymbol) {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionData> connectorResult =
                dbManager.getFunction(moduleId.orgName(), moduleId.packageName(), NewConnectionBuilder.INIT_SYMBOL,
                        FunctionData.Kind.CONNECTOR, null);
        if (connectorResult.isEmpty()) {
            return List.of();
        }

        FunctionData connector = connectorResult.get();
        List<FunctionData> connectorActions = dbManager.getConnectorActions(connector.functionId());

        List<Item> availableNodes = new ArrayList<>();
        for (FunctionData connectorAction : connectorActions) {
            if (connectorAction.kind() == FunctionData.Kind.REMOTE) {
                availableNodes.add(getActionNode(connectorAction, connector, parentSymbol).buildAvailableNode());
            } else if (connectorAction.kind() == FunctionData.Kind.FUNCTION) {
                availableNodes.add(getMethodCallNode(connectorAction, connector, parentSymbol).buildAvailableNode());
            } else {
                if (isHttpModule(connector) && HTTP_REMOTE_METHOD_SKIP_LIST.contains(connectorAction.name())) {
                    continue;
                }
                availableNodes.add(
                        getResourceActionNode(connectorAction, connector, parentSymbol).buildAvailableNode());
            }
        }
        return availableNodes;
    }

    private static NodeBuilder getMethodCallNode(FunctionData functionData, FunctionData connector,
                                                 String parentSymbol) {
        NodeBuilder methodCallBuilder = NodeBuilder.getNodeFromKind(NodeKind.METHOD_CALL);
        methodCallBuilder
                .metadata()
                    .label(functionData.name())
                    .icon(CommonUtils.generateIcon(connector.org(), connector.packageName(), connector.version()))
                    .description(functionData.description())
                    .stepOut()
                .codedata()
                    .node(NodeKind.METHOD_CALL)
                    .org(connector.org())
                    .module(connector.packageName())
                    .symbol(functionData.name())
                    .parentSymbol(parentSymbol)
                    .id(functionData.functionId());
        return methodCallBuilder;
    }

    private static NodeBuilder getActionNode(FunctionData connectorAction, FunctionData connector,
                                             String parentSymbol) {
        NodeBuilder actionBuilder = NodeBuilder.getNodeFromKind(NodeKind.REMOTE_ACTION_CALL);
        actionBuilder
                .metadata()
                    .label(connectorAction.name())
                    .icon(CommonUtils.generateIcon(connector.org(), connector.packageName(), connector.version()))
                    .description(connectorAction.description())
                    .stepOut()
                .codedata()
                    .node(NodeKind.REMOTE_ACTION_CALL)
                    .org(connector.org())
                    .module(connector.packageName())
                    .object(NewConnectionBuilder.CLIENT_SYMBOL)
                    .symbol(connectorAction.name())
                    .parentSymbol(parentSymbol)
                    .id(connectorAction.functionId());
        return actionBuilder;
    }

    private static NodeBuilder getResourceActionNode(FunctionData connectorAction, FunctionData connector,
                                                     String parentSymbol) {
        NodeBuilder actionBuilder = NodeBuilder.getNodeFromKind(NodeKind.RESOURCE_ACTION_CALL);
        String label = connectorAction.name() + (isHttpModule(connector) ? "" : connectorAction.resourcePath());
        actionBuilder
                .metadata()
                    .label(label)
                    .icon(CommonUtils.generateIcon(connector.org(), connector.packageName(), connector.version()))
                    .description(connectorAction.description())
                    .functionKind(FunctionData.Kind.RESOURCE.name())
                    .stepOut()
                .codedata()
                    .node(NodeKind.RESOURCE_ACTION_CALL)
                    .org(connector.org())
                    .module(connector.packageName())
                    .object(NewConnectionBuilder.CLIENT_SYMBOL)
                    .symbol(connectorAction.name())
                    .parentSymbol(parentSymbol)
                    .resourcePath(connectorAction.resourcePath())
                    .id(connectorAction.functionId());
        return actionBuilder;
    }

    private static boolean isHttpModule(FunctionData connector) {
        return connector.org().equals(BALLERINA_ORG) && connector.packageName().equals(HTTP_MODULE);
    }
}
