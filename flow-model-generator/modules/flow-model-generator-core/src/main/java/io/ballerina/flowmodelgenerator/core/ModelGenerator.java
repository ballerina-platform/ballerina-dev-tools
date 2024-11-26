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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassFieldSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.model.Diagram;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generator for the flow model.
 *
 * @since 1.4.0
 */
public class ModelGenerator {

    private final SemanticModel semanticModel;
    private final Path filePath;
    private final Gson gson;
    private final Project project;

    public ModelGenerator(Project project, SemanticModel model, Path filePath) {
        this.semanticModel = model;
        this.filePath = filePath;
        this.project = project;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    /**
     * Generates a flow model for the given canvas node.
     *
     * @return JSON representation of the flow model
     */
    public JsonElement getFlowModel(Document document, LineRange lineRange, Document dataMappingDoc) {
        // Obtain the code block representing the canvas
        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        NonTerminalNode canvasNode = modulePartNode.findNode(TextRange.from(start, end - start), true);

        // Obtain the connections visible at the module-level
        List<FlowNode> moduleConnections =
                semanticModel.visibleSymbols(document, canvasNode.lineRange().startLine()).stream()
                        .flatMap(symbol -> buildConnection(symbol).stream())
                        .sorted(Comparator.comparing(
                                node -> Optional.ofNullable(node.properties().get(Property.VARIABLE_KEY))
                                        .map(property -> property.value().toString())
                                        .orElse("")))
                        .toList();

        // Obtain the data mapping function names
        Map<String, LineRange> dataMappings = new HashMap<>();
        if (dataMappingDoc != null) {
            ModulePartNode dataMappingModulePartNode = dataMappingDoc.syntaxTree().rootNode();
            for (ModuleMemberDeclarationNode member : dataMappingModulePartNode.members()) {
                if (member.kind() == SyntaxKind.FUNCTION_DEFINITION) {
                    FunctionDefinitionNode functionNode = (FunctionDefinitionNode) member;
                    String functionName = functionNode.functionName().text();
                    LineRange functionLineRange = functionNode.lineRange();
                    dataMappings.put(functionName, functionLineRange);
                }
            }
        }

        // Analyze the code block to find the flow nodes
        CodeAnalyzer codeAnalyzer = new CodeAnalyzer(project, semanticModel, Property.LOCAL_SCOPE, dataMappings,
                textDocument, ModuleInfo.from(document.module().descriptor()), true);
        canvasNode.accept(codeAnalyzer);

        // Generate the flow model
        Diagram diagram = new Diagram(filePath.toString(), codeAnalyzer.getFlowNodes(), moduleConnections);
        return gson.toJsonTree(diagram);
    }

    public JsonElement getModuleNodes() {
        List<FlowNode> connectionsList = semanticModel.moduleSymbols().stream()
                .flatMap(symbol -> buildConnection(symbol).stream())
                .sorted(Comparator.comparing(
                        node -> Optional.ofNullable(node.properties().get(Property.VARIABLE_KEY))
                                .map(property -> property.value().toString())
                                .orElse("")))
                .toList();
        Diagram diagram = new Diagram(filePath.toString(), List.of(), connectionsList);
        return gson.toJsonTree(diagram);
    }

    /**
     * Builds a client from the given type symbol.
     *
     * @return the client if the type symbol is a client, otherwise empty
     */
    private Optional<FlowNode> buildConnection(Symbol symbol) {
        Function<NonTerminalNode, NonTerminalNode> getStatementNode;
        NonTerminalNode statementNode;
        TypeSymbol typeSymbol;
        String scope;
        Document document;

        switch (symbol.kind()) {
            case VARIABLE -> {
                getStatementNode = (NonTerminalNode node) -> node.parent().parent();
                typeSymbol = ((VariableSymbol) symbol).typeDescriptor();
                scope = Property.GLOBAL_SCOPE;
            }
            case CLASS_FIELD -> {
                getStatementNode = (NonTerminalNode node) -> node;
                typeSymbol = ((ClassFieldSymbol) symbol).typeDescriptor();
                scope = Property.SERVICE_SCOPE;
            }
            default -> {
                return Optional.empty();
            }
        }
        try {
            TypeSymbol typeDescriptorSymbol = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            if (typeDescriptorSymbol.kind() != SymbolKind.CLASS ||
                    !((ClassSymbol) typeDescriptorSymbol).qualifiers().contains(Qualifier.CLIENT)) {
                return Optional.empty();
            }
            Location location = symbol.getLocation().orElseThrow();
            DocumentId documentId = project.documentId(
                    project.kind() == ProjectKind.SINGLE_FILE_PROJECT ? project.sourceRoot() :
                            project.sourceRoot().resolve(location.lineRange().fileName()));
            document = project.currentPackage().getDefaultModule().document(documentId);
            NonTerminalNode childNode =
                    symbol.getLocation().map(loc -> CommonUtils.getNode(document.syntaxTree(), loc.textRange()))
                            .orElseThrow();
            statementNode = getStatementNode.apply(childNode);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
        if (statementNode == null) {
            return Optional.empty();
        }
        CodeAnalyzer codeAnalyzer = new CodeAnalyzer(project, semanticModel, scope, Map.of(),
                document.textDocument(), ModuleInfo.from(document.module().descriptor()), false);
        statementNode.accept(codeAnalyzer);
        List<FlowNode> connections = codeAnalyzer.getFlowNodes();
        return connections.stream().findFirst();
    }
}
