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
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.central.Central;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generates available nodes for a given position in the diagram.
 *
 * @since 1.4.0
 */
public class AvailableNodesGenerator {

    private final Category.Builder rootBuilder;
    private final Gson gson;
    private final Central central;

    public AvailableNodesGenerator() {
        this.rootBuilder = new Category.Builder(Category.Name.ROOT, null);
        this.gson = new Gson();
        central = CentralProxy.getInstance();
    }

    public JsonArray getAvailableNodes() {
        List<Item> items = this.rootBuilder.build().items();
        items.addAll(central.getAvailableConnections());
        items.addAll(central.getFunctions());
        return gson.toJsonTree(items).getAsJsonArray();
    }

    public JsonArray getAvailableNodes(Document document, LinePosition cursorPosition, SemanticModel semanticModel) {
        List<Item> items = new ArrayList<>();
        items.addAll(getAvailableFlowNodes(document, cursorPosition, semanticModel));
        items.addAll(central.getAvailableConnections());
        items.addAll(central.getFunctions());
        return gson.toJsonTree(items).getAsJsonArray();
    }

    private List<Item> getAvailableFlowNodes(Document document, LinePosition cursorPosition,
                                             SemanticModel semanticModel) {
        int txtPos = document.textDocument().textPositionFrom(cursorPosition);
        TextRange range = TextRange.from(txtPos, 0);
        NonTerminalNode nonTerminalNode = ((ModulePartNode) document.syntaxTree().rootNode()).findNode(range);

        while (nonTerminalNode != null) {
            SyntaxKind kind = nonTerminalNode.kind();
            switch (kind) {
                case WHILE_STATEMENT, FOREACH_STATEMENT -> {
                    setAvailableNodesForIteratingBlock(nonTerminalNode, semanticModel);
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
        setStopNode(node, semanticModel);
    }

    private void setAvailableNodesForIteratingBlock(NonTerminalNode node, SemanticModel semanticModel) {
        setDefaultNodes();
        setStopNode(node, semanticModel);
        this.rootBuilder
            .stepIn(Category.Name.FLOW)
                .stepIn(Category.Name.ITERATION)
                    .node(FlowNode.Kind.BREAK)
                    .node(FlowNode.Kind.CONTINUE)
                .stepOut()
            .stepOut();
    }

    private void setStopNode(NonTerminalNode node, SemanticModel semanticModel) {
        Node parent = node;
        while (parent != null) {
            if (parent.kind() == SyntaxKind.FUNCTION_DEFINITION) {
                FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode) parent;
                Optional<TypeSymbol> optRetTypeSymbol = ((FunctionSymbol) semanticModel.symbol(funcDefNode).get())
                        .typeDescriptor().returnTypeDescriptor();
                if (optRetTypeSymbol.isEmpty() || optRetTypeSymbol.get().subtypeOf(semanticModel.types().NIL)) {
                    this.rootBuilder
                        .stepIn(Category.Name.FLOW)
                            .stepIn(Category.Name.CONTROL)
                                .node(FlowNode.Kind.STOP)
                            .stepOut()
                        .stepOut();
                }
                break;
            }
            parent = parent.parent();
        }
    }

    private void setDefaultNodes() {
        this.rootBuilder
            .stepIn(Category.Name.FLOW)
                .stepIn(Category.Name.BRANCH)
                    .node(FlowNode.Kind.IF)
                .stepOut()
                .stepIn(Category.Name.ITERATION)
                    .node(FlowNode.Kind.WHILE)
                    .node(FlowNode.Kind.FOREACH)
                .stepOut()
                .stepIn(Category.Name.CONTROL)
                    .node(FlowNode.Kind.RETURN)
                .stepOut()
            .stepOut()
            .stepIn(Category.Name.DATA)
                .node(FlowNode.Kind.NEW_DATA)
                .node(FlowNode.Kind.UPDATE_DATA)
            .stepOut()
            .stepIn(Category.Name.ERROR_HANDLING)
                .node(FlowNode.Kind.ERROR_HANDLER)
                .node(FlowNode.Kind.PANIC)
            .stepOut()
            .stepIn(Category.Name.CONCURRENCY)
                .node(FlowNode.Kind.TRANSACTION)
                .node(FlowNode.Kind.LOCK)
                .node(FlowNode.Kind.START)
            .stepOut();
    }
}
