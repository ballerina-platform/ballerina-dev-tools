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
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.projects.Document;
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
public class DataMapper extends NodeBuilder {

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
        properties()
                .data(null)
                .custom(FUNCTION_NAME_KEY, FUNCTION_NAME_LABEL, FUNCTION_NAME_DOC, Property.ValueType.IDENTIFIER,
                        null, "transform", false);

        // Obtain the visible variables to the cursor position
        WorkspaceManager workspaceManager = context.workspaceManager();
        SemanticModel semanticModel;
        Document document;
        String projectName;
        try {
            workspaceManager.loadProject(context.filePath());
            semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            document = workspaceManager.document(context.filePath()).orElseThrow();
            projectName = CommonUtils.getProjectName(document);
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }

        Set<String> visibleVariables = new TreeSet<>();
        Set<String> visibleRecordTypes = new TreeSet<>();

        for (Symbol symbol : semanticModel.visibleSymbols(document, context.position())) {
            if (symbol.kind() == SymbolKind.VARIABLE &&
                    symbol.getName().filter(name -> !name.equals("self")).isPresent()) {
                getVariableSignature(semanticModel, projectName, (VariableSymbol) symbol).ifPresent(
                        visibleVariables::add);
            } else if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
                getRecordTypeSignature((TypeDefinitionSymbol) symbol).ifPresent(visibleRecordTypes::add);
            }
        }

        properties().custom(INPUTS_KEY, INPUTS_LABEL, INPUTS_DOC, Property.ValueType.MULTIPLE_SELECT,
                new ArrayList<>(visibleVariables), "", false);
        properties().custom(OUTPUT_KEY, OUTPUT_LABEL, OUTPUT_DOC, Property.ValueType.SINGLE_SELECT,
                new ArrayList<>(visibleRecordTypes), "", false);
    }

    private static Optional<String> getVariableSignature(SemanticModel semanticModel, String projectName,
                                                         VariableSymbol symbol) {
        Optional<String> name = symbol.getName();
        String typeSignature = CommonUtils.getTypeSignature(semanticModel, symbol.typeDescriptor(), false, projectName);
        return name.map(s -> typeSignature + " " + s);
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

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> functionName = sourceBuilder.flowNode.getProperty(FUNCTION_NAME_KEY);
        if (functionName.isEmpty()) {
            throw new IllegalStateException("Function name must be defined for a data mapper node");
        }

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
                        RecordUtil.getFillAllRecordFieldInsertText(((RecordTypeSymbol) typeSymbol).fieldDescriptors());
            }
        }

        sourceBuilder.token()
                .keyword(SyntaxKind.FUNCTION_KEYWORD)
                .name(functionName.get().value().toString())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(String.join(", ", inputArray))
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .keyword(SyntaxKind.RETURNS_KEYWORD)
                .name(output.get().value().toString())
                .keyword(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN)
                .openBrace()
                .name(bodyText)
                .closeBrace()
                .endOfStatement()
                .stepOut()
                .textEdit(false, "data_mappings.bal", false);

        Optional<Property> variable = sourceBuilder.flowNode.getProperty(Property.VARIABLE_KEY);
        if (variable.isEmpty()) {
            throw new IllegalStateException("Variable must be defined for a data mapper node");
        }

        String functionParameters = inputArray.stream()
                .map(item -> item.split(" ")[1])
                .collect(Collectors.joining(","));
        sourceBuilder.newVariable(OUTPUT_KEY)
                .token()
                .name(functionName.get().value().toString())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN)
                .name(functionParameters)
                .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                .endOfStatement()
                .stepOut()
                .textEdit(false);

        return sourceBuilder.build();
    }
}
