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
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
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
import io.ballerina.flowmodelgenerator.core.model.node.AgentBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.NPFunctionCall;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.projects.Document;
import io.ballerina.projects.Package;
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
    private final Package pkg;
    private final Gson gson;
    private static final String BALLERINA_ORG = "ballerina";
    private static final String HTTP_MODULE = "http";
    private static final List<String> HTTP_REMOTE_METHOD_SKIP_LIST = List.of("get", "put", "post", "head",
            "delete", "patch", "options");
    private static final String BALLERINAX = "ballerinax";
    public static final String AI_AGENT = "ai";

    public AvailableNodesGenerator(SemanticModel semanticModel, Document document, Package pkg) {
        this.rootBuilder = new Category.Builder(null).name(Category.Name.ROOT);
        this.gson = new Gson();
        this.semanticModel = semanticModel;
        this.document = document;
        this.pkg = pkg;
    }

    public JsonArray getAvailableNodes(LinePosition position) {
        List<Category> connections = new ArrayList<>();
        List<Symbol> symbols = semanticModel.visibleSymbols(document, position);
        for (Symbol symbol : symbols) {
            Optional<Category> connection = getConnection(symbol);
            if (connection.isEmpty()) {
                continue;
            }
            connections.add(connection.get());
        }
        connections.sort(Comparator.comparing(connection -> connection.metadata().label()));
        this.rootBuilder.stepIn(Category.Name.CONNECTIONS).items(new ArrayList<>(connections)).stepOut();

        List<Item> items = new ArrayList<>();
        items.addAll(getAvailableFlowNodes(position));
        items.addAll(LocalIndexCentral.getInstance().getFunctions());
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
                        .label("Call Function")
                        .description("Both project and utility functions")
                        .build(),
                new Codedata.Builder<>(null)
                        .node(NodeKind.FUNCTION)
                        .build(),
                true
        );

        AvailableNode npFunction = new AvailableNode(
                new Metadata.Builder<>(null)
                        .label(NPFunctionCall.LABEL)
                        .description(NPFunctionCall.DESCRIPTION)
                        .icon(Constants.NaturalFunctions.ICON)
                        .build(),
                new Codedata.Builder<>(null)
                        .node(NodeKind.NP_FUNCTION)
                        .build(),
                true
        );

        AvailableNode agentCall = new AvailableNode(
                new Metadata.Builder<>(null)
                        .label(AgentBuilder.LABEL)
                        .description(AgentBuilder.DESCRIPTION)
                        .build(),
                new Codedata.Builder<>(null)
                        .node(NodeKind.AGENT_CALL)
                        .org(BALLERINAX)
                        .module(AI_AGENT)
                        .symbol("run")
                        .object("Agent")
                        .build(),
                true
        );

        this.rootBuilder.stepIn(Category.Name.STATEMENT)
                .node(NodeKind.VARIABLE)
                .node(NodeKind.ASSIGN)
                .node(function)
                .node(NodeKind.DATA_MAPPER_CALL)
                .node(npFunction)
                .node(agentCall);

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
            TypeReferenceTypeSymbol typeDescriptorSymbol;
            if (symbol instanceof VariableSymbol variableSymbol) {
                typeDescriptorSymbol = (TypeReferenceTypeSymbol) variableSymbol.typeDescriptor();
            } else if (symbol instanceof ParameterSymbol parameterSymbol) {
                typeDescriptorSymbol = (TypeReferenceTypeSymbol) parameterSymbol.typeDescriptor();
            } else {
                return Optional.empty();
            }

            ClassSymbol classSymbol = (ClassSymbol) typeDescriptorSymbol.typeDescriptor();
            if (!(classSymbol.qualifiers().contains(Qualifier.CLIENT))) {
                return Optional.empty();
            }
            String parentSymbolName = symbol.getName().orElseThrow();
            String className = classSymbol.getName().orElseThrow();
            ModuleInfo moduleInfo = classSymbol.getModule()
                    .map(moduleSymbol -> ModuleInfo.from(moduleSymbol.id()))
                    .orElse(null);

            FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                    .parentSymbol(classSymbol)
                    .parentSymbolType(className)
                    .project(pkg.project())
                    .moduleInfo(moduleInfo);

            // Obtain methods of the connector
            List<FunctionData> methodFunctionsData = functionDataBuilder.buildChildNodes();

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
                    FunctionData.Kind kind = methodFunction.kind();
                    if (kind == FunctionData.Kind.REMOTE) {
                        nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.REMOTE_ACTION_CALL);
                    } else if (kind == FunctionData.Kind.FUNCTION) {
                        nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.METHOD_CALL);
                    } else {
                        throw new IllegalStateException("Unexpected value: " + kind);
                    }
                }

                Item node = nodeBuilder
                        .metadata()
                            .label(label)
                            .icon(CommonUtils.generateIcon(org, packageName, version))
                            .description(methodFunction.description())
                            .stepOut()
                        .codedata()
                            .org(org)
                            .module(functionDataBuilder.isLocal() ? moduleInfo.moduleName() : packageName)
                            .object(className)
                            .symbol(methodFunction.name())
                            .version(version)
                            .parentSymbol(parentSymbolName)
                            .resourcePath(methodFunction.resourcePath())
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
