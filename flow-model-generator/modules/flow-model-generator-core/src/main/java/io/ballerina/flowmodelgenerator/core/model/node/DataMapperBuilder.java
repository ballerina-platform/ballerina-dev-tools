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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;
import org.ballerinalang.langserver.common.utils.RecordUtil;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Represents the properties of a data mapper node in the flow model.
 *
 * @since 1.4.0
 */
public class DataMapperBuilder extends NodeBuilder {

    public static final String LABEL = "Data Mapper";
    public static final String DESCRIPTION = "Map data from multiple variables to a record type";

    public static final String FUNCTION_NAME_KEY = "functionName";
    public static final String FUNCTION_NAME_LABEL = "Data mapper name";
    public static final String FUNCTION_NAME_DOC = "Name of the data mapper function";

    public static final String INPUTS_KEY = "inputs";
    public static final String INPUTS_LABEL = "Inputs";
    public static final String INPUTS_DOC = "Input variables of the data mapper function";

    public static final String OUTPUT_KEY = "output";
    public static final String OUTPUT_LABEL = "Output";
    public static final String OUTPUT_DOC = "Output of the data mapper function";

    public static final String VIEW_KEY = "view";
    public static final String VIEW_LABEL = "View";
    public static final String VIEW_DOC = "Function definition location";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.DATA_MAPPER);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Set<String> allVisibleSymbolNames = context.getAllVisibleSymbolNames();
        properties().data(null, allVisibleSymbolNames);
        properties().custom()
                .metadata()
                    .label(FUNCTION_NAME_LABEL)
                    .description(FUNCTION_NAME_DOC)
                    .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .value(NameUtil.generateTypeName("transform", allVisibleSymbolNames))
                .editable()
                .stepOut()
                .addProperty(FUNCTION_NAME_KEY);

        // Obtain the visible variables to the cursor position
        WorkspaceManager workspaceManager = context.workspaceManager();
        SemanticModel semanticModel;
        Document document;
        ModuleInfo currentModuleInfo;
        try {
            workspaceManager.loadProject(context.filePath());
            semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            document = workspaceManager.document(context.filePath()).orElseThrow();
            currentModuleInfo = ModuleInfo.from(document.module().descriptor());
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }

        Set<String> visibleVariables = new TreeSet<>();
        Set<String> visibleRecordTypes = new TreeSet<>();

        for (Symbol symbol : semanticModel.visibleSymbols(document, context.position())) {
            if (symbol.kind().equals(SymbolKind.VARIABLE) &&
                    symbol.getName().filter(name -> !name.equals("self")).isPresent()) {
                getVariableSignature(semanticModel, currentModuleInfo, symbol.getName().orElse(""),
                        ((VariableSymbol) symbol).typeDescriptor()).ifPresent(
                        visibleVariables::add);
            } else if (symbol.kind() == SymbolKind.PARAMETER) {
                getVariableSignature(semanticModel, currentModuleInfo, symbol.getName().orElse(""),
                        ((ParameterSymbol) symbol).typeDescriptor()).ifPresent(
                        visibleVariables::add);
            } else if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
                getRecordTypeSignature((TypeDefinitionSymbol) symbol).ifPresent(visibleRecordTypes::add);
            }
        }

        properties().custom()
                .metadata()
                    .label(INPUTS_LABEL)
                    .description(INPUTS_DOC)
                    .stepOut()
                .type(Property.ValueType.MULTIPLE_SELECT)
                .value("")
                .typeConstraint(new ArrayList<>(visibleVariables))
                .optional(false)
                .editable()
                .stepOut()
                .addProperty(INPUTS_KEY);
        properties().custom()
                .metadata()
                    .label(OUTPUT_LABEL)
                    .description(OUTPUT_DOC)
                    .stepOut()
                .type(Property.ValueType.SINGLE_SELECT)
                .value("")
                .typeConstraint(new ArrayList<>(visibleRecordTypes))
                .optional(false)
                .editable()
                .stepOut()
                .addProperty(OUTPUT_KEY);
    }

    private static Optional<String> getVariableSignature(SemanticModel semanticModel, ModuleInfo moduleInfo,
                                                         String name, TypeSymbol typeSymbol) {
        if (name == null || name.isEmpty()) {
            return Optional.empty();
        }
        String typeSignature =
                CommonUtils.getTypeSignature(semanticModel, typeSymbol, false, moduleInfo);
        return Optional.of(typeSignature + " " + name);
    }

    private static Optional<String> getRecordTypeSignature(TypeDefinitionSymbol symbol) {
        if (symbol.typeDescriptor().typeKind() != TypeDescKind.RECORD) {
            return Optional.empty();
        }
        Optional<String> moduleName = symbol.getModule().flatMap(Symbol::getName);

        // TODO: Make this more scalable
        if (moduleName.isPresent() && moduleName.get().equals("lang.annotations")) {
            return Optional.empty();
        }
        return symbol.getName();
    }

    private Optional<LineRange> getTransformFunctionLocation(SourceBuilder sourceBuilder, String functionNameString) {
        Project project;
        try {
            project = sourceBuilder.workspaceManager.loadProject(sourceBuilder.filePath);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return Optional.empty();
        }
        Optional<SemanticModel> semanticModel = sourceBuilder.workspaceManager.semanticModel(sourceBuilder.filePath);
        Optional<Location> location = semanticModel.flatMap(model -> model.moduleSymbols().parallelStream()
                .filter(symbol -> symbol.kind() == SymbolKind.FUNCTION && symbol.nameEquals(functionNameString))
                .findAny()
                .flatMap(Symbol::getLocation));
        if (location.isEmpty()) {
            return Optional.empty();
        }
        Document document = CommonUtils.getDocument(project, location.get());
        ModulePartNode node = document.syntaxTree().rootNode();
        if (node.kind() != SyntaxKind.MODULE_PART) {
            return Optional.empty();
        }
        return node.members().stream().parallel()
                .filter(member -> member.kind() == SyntaxKind.FUNCTION_DEFINITION &&
                        ((FunctionDefinitionNode) member).functionName().text().strip().equals(functionNameString))
                .findAny()
                .map(Node::lineRange);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> functionName = sourceBuilder.flowNode.getProperty(FUNCTION_NAME_KEY);
        if (functionName.isEmpty()) {
            throw new IllegalStateException("Function name must be defined for a data mapper node");
        }
        String functionNameString = functionName.get().value().toString();

        Optional<Property> inputs = sourceBuilder.flowNode.getProperty(INPUTS_KEY);
        if (inputs.isEmpty()) {
            throw new IllegalStateException("Inputs must be defined for a data mapper node");
        }
        List<String> inputArray = new ArrayList<>();
        if (inputs.get().value() instanceof List) {
            for (Object input : (List<?>) inputs.get().value()) {
                inputArray.add(input.toString());
            }
        }

        Optional<Property> output = sourceBuilder.flowNode.getProperty(OUTPUT_KEY);
        if (output.isEmpty()) {
            throw new IllegalStateException("Output must be defined for a data mapper node");
        }

        String bodyText = "";
        Optional<Symbol> recordSymbol = sourceBuilder.getTypeSymbol(output.get().value().toString());
        if (recordSymbol.isPresent()) {
            TypeSymbol typeSymbol = ((TypeDefinitionSymbol) (recordSymbol.get())).typeDescriptor();
            if (typeSymbol.typeKind() == TypeDescKind.RECORD) {
                bodyText =
                        RecordUtil.getFillAllRecordFieldInsertText(
                                ((RecordTypeSymbol) typeSymbol).fieldDescriptors());
            }
        }

        sourceBuilder.token()
                .keyword(SyntaxKind.FUNCTION_KEYWORD)
                .name(functionNameString)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(String.join(", ", inputArray))
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .keyword(SyntaxKind.RETURNS_KEYWORD)
                .name(output.get().value().toString())
                .keyword(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN)
                .openBrace()
                .name(bodyText)
                .closeBrace()
                .endOfStatement();

        getTransformFunctionLocation(sourceBuilder, functionNameString).ifPresentOrElse(
                lineRange -> sourceBuilder.textEdit(false, "data_mappings.bal", lineRange, false),
                () -> sourceBuilder.textEdit(false, "data_mappings.bal", false));

        Optional<Property> variable = sourceBuilder.flowNode.getProperty(Property.VARIABLE_KEY);
        if (variable.isEmpty()) {
            throw new IllegalStateException("Variable must be defined for a data mapper node");
        }

        String functionParameters = inputArray.stream()
                .map(item -> item.split(" ")[1])
                .collect(Collectors.joining(","));
        sourceBuilder.newVariable(OUTPUT_KEY)
                .token()
                .name(functionNameString)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(functionParameters)
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement()
                .stepOut()
                .textEdit(false);

        return sourceBuilder.build();
    }
}
