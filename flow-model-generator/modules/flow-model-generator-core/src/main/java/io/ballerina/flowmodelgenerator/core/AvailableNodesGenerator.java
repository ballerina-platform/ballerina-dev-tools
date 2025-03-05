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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
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
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
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
//        genAvailableAgents(items);
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

//    private void genAvailableAgents(List<Item> items) {
//        List<Symbol> symbols = semanticModel.moduleSymbols();
//        List<Item> agentItems = new ArrayList<>();
//        for (Symbol symbol : symbols) {
//            if (symbol.kind() != SymbolKind.VARIABLE) {
//                continue;
//            }
//            VariableSymbol variableSymbol = (VariableSymbol) symbol;
//            String typeName = variableSymbol.typeDescriptor().getName().orElse("");
//            if (agents.contains(typeName)) {
//                Metadata metadata = new Metadata.Builder<>(null)
//                        .label(variableSymbol.getName().orElse(""))
//                        .build();
//                FunctionData functionResult = new FunctionData(-1, "run", "Run agent", "error?", AI_AGENT, WSO2,
//                        AI_VERSION, "",
//                        FunctionData.Kind.FUNCTION, true, false);
//                NodeBuilder methodCallBuilder = NodeBuilder.getNodeFromKind(NodeKind.AGENT_CALL);
//                methodCallBuilder
//                        .metadata()
//                            .label(functionResult.name())
//                            .icon(CommonUtils.generateIcon(WSO2, AI_AGENT, AI_VERSION))
//                            .description(functionResult.description())
//                            .stepOut()
//                        .codedata()
//                            .node(NodeKind.AGENT_CALL)
//                            .org(WSO2)
//                            .module(AI_AGENT)
//                            .version(AI_VERSION)
//                            .symbol(functionResult.name())
//                            .id(functionResult.functionId());
//                agentItems.add(new Category(metadata, List.of(methodCallBuilder.buildAvailableNode())));
//            }
//        }
//
//        if (!agentItems.isEmpty()) {
//            items.add(this.rootBuilder.stepIn(Category.Name.AGENTS).items(agentItems).build());
//        }
//    }

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
            ClassSymbol classSymbol = (ClassSymbol) typeDescriptorSymbol.typeDescriptor();
            if (!(classSymbol.qualifiers().contains(Qualifier.CLIENT))) {
                return Optional.empty();
            }
            String parentSymbolName = symbol.getName().orElseThrow();
            String className = classSymbol.getName().orElseThrow();

            // Obtain methods of the connector
            List<FunctionData> methodFunctionsData = new FunctionDataBuilder()
                    .parentSymbol(classSymbol)
                    .buildChildNodes();

            List<Item> methods = new ArrayList<>();
            for (FunctionData methodFunction : methodFunctionsData) {
                String org = methodFunction.org();
                String packageName = methodFunction.packageName();
                String version = methodFunction.version();
                boolean isHttpModule = org.equals(BALLERINA_ORG) && packageName.equals(HTTP_MODULE);

                NodeBuilder nodeBuilder;
                String label;
                if (methodFunction.kind() == FunctionData.Kind.RESOURCE) {
                    // TODO: Move this logic to the index
                    if (isHttpModule && HTTP_REMOTE_METHOD_SKIP_LIST.contains(methodFunction.name())) {
                        continue;
                    }
                    label = methodFunction.name() + (isHttpModule ? "" : methodFunction.resourcePath());
                    nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.RESOURCE_ACTION_CALL);
                } else {
                    label = methodFunction.name();
                    nodeBuilder = switch (methodFunction.kind()) {
                        case REMOTE -> NodeBuilder.getNodeFromKind(NodeKind.REMOTE_ACTION_CALL);
                        case FUNCTION -> NodeBuilder.getNodeFromKind(NodeKind.METHOD_CALL);
                        default -> throw new IllegalStateException("Unexpected value: " + methodFunction.kind());
                    };
                }

                Item node = nodeBuilder
                        .metadata()
                            .label(label)
                            .icon(CommonUtils.generateIcon(org, packageName, version))
                            .description(methodFunction.description())
                            .stepOut()
                        .codedata()
                            .org(org)
                            .module(packageName)
                            .object(className)
                            .symbol(methodFunction.name())
                            .version(version)
                            .parentSymbol(parentSymbolName)
                            .resourcePath(methodFunction.resourcePath())
                            .id(methodFunction.functionId())
                            .stepOut()
                        .buildAvailableNode();
                methods.add(node);
            }

            Metadata metadata = new Metadata.Builder<>(null)
                    .label(parentSymbolName)
                    .build();
            return Optional.of(new Category(metadata, methods));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
