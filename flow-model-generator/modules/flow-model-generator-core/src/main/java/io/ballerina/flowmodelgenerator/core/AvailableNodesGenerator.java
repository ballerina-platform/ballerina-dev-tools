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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.flowmodelgenerator.core.central.Central;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Generates available nodes for a given position in the diagram.
 *
 * @since 1.4.0
 */
public class AvailableNodesGenerator {

    private final Category.Builder rootBuilder;
    private final SemanticModel semanticModel;
    private final Document document;
    private final Gson gson;
    private final Central central;

    public AvailableNodesGenerator(SemanticModel semanticModel, Document document) {
        this.rootBuilder = new Category.Builder(Category.Name.ROOT, null);
        this.gson = new Gson();
        this.central = CentralProxy.getInstance();
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

        // TODO: Need to ensure that this is only called once.
        initializeCommonNodes();
        List<Item> items = this.rootBuilder.build().items();
        items.addAll(central.getFunctions());

        return gson.toJsonTree(items).getAsJsonArray();
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
            Codedata codedata = new Codedata.Builder<>(null)
                    .node(FlowNode.Kind.NEW_CONNECTION)
                    .org(moduleSymbol.id().orgName())
                    .module(moduleSymbol.getName().orElseThrow())
                    .object("Client")
                    .symbol("init")
                    .build();
            List<Item> connections = central.getConnections(codedata);

            Metadata metadata = new Metadata.Builder<>(null)
                    .label(symbol.getName().orElseThrow())
                    .build();
            return Optional.of(new Category(metadata, connections));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private void initializeCommonNodes() {
        // Initialize the builder with the common nodes
        this.rootBuilder
                .stepIn(Category.Name.FLOW)
                    .stepIn(Category.Name.BRANCH)
                        .node(FlowNode.Kind.IF)
                    .stepOut()
                    .stepIn(Category.Name.ITERATION)
                        .node(FlowNode.Kind.WHILE)
                        .node(FlowNode.Kind.FOREACH)
                        .node(FlowNode.Kind.BREAK)
                        .node(FlowNode.Kind.CONTINUE)
                    .stepOut()
                    .stepIn(Category.Name.CONTROL)
                        .node(FlowNode.Kind.RETURN)
                        .node(FlowNode.Kind.STOP)
                    .stepOut()
                .stepOut()
                .stepIn(Category.Name.DATA)
                    .node(FlowNode.Kind.NEW_DATA)
                    .node(FlowNode.Kind.UPDATE_DATA)
                    .node(FlowNode.Kind.DATA_MAPPER)
                .stepOut()
                .stepIn(Category.Name.ERROR_HANDLING)
                    .node(FlowNode.Kind.ERROR_HANDLER)
                    .node(FlowNode.Kind.PANIC)
                .stepOut();
                // TODO: Tracked with: #65
//                .stepIn(Category.Name.CONCURRENCY)
//                    .node(FlowNode.Kind.TRANSACTION)
//                    .node(FlowNode.Kind.LOCK)
//                    .node(FlowNode.Kind.START)
//                .stepOut();
    }
}
