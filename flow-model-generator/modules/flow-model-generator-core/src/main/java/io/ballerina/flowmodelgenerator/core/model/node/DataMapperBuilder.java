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
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;
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

/**
 * Represents the properties of a data mapper node in the flow model.
 *
 * @since 2.0.0
 * @deprecated Use {@link DataMapperDefinitionBuilder} instead. The implementation is kept in case this feature is
 * needed in the future.
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
        try {
            workspaceManager.loadProject(context.filePath());
            semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            document = workspaceManager.document(context.filePath()).orElseThrow();
        } catch (WorkspaceDocumentException | EventSyncException e) {
            throw new RuntimeException(e);
        }

        Set<String> visibleTypes = new TreeSet<>();
        for (Symbol symbol : semanticModel.visibleSymbols(document, context.position())) {
            if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
                addDataMappingCapableTypes(visibleTypes, symbol, ((TypeDefinitionSymbol) symbol).typeDescriptor());
            }
        }

        properties().custom()
                .metadata()
                    .label(INPUTS_LABEL)
                    .description(INPUTS_DOC)
                    .stepOut()
                .type(Property.ValueType.MULTIPLE_SELECT)
                .value("")
                .typeConstraint(new ArrayList<>(visibleTypes))
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
                .typeConstraint(new ArrayList<>(visibleTypes))
                .optional(false)
                .editable()
                .stepOut()
                .addProperty(OUTPUT_KEY);
    }

    private void addDataMappingCapableTypes(Set<String> types, Symbol parentSymbol, TypeSymbol typeSymbol) {
        TypeSymbol rawType = CommonUtils.getRawType(typeSymbol);
        switch (rawType.typeKind()) {
            case ARRAY ->
                    addDataMappingCapableTypes(types, parentSymbol, ((ArrayTypeSymbol) rawType).memberTypeDescriptor());
            case RECORD -> {
                Optional<String> moduleName = parentSymbol.getModule().flatMap(Symbol::getName);
                if (moduleName.isPresent() && moduleName.get().equals("lang.annotations")) {
                    break;
                }
                parentSymbol.getName().ifPresent(types::add);
            }
            case NIL, BOOLEAN, INT, FLOAT, DECIMAL, BYTE, STRING, JSON -> parentSymbol.getName().ifPresent(types::add);
            default -> {
            }
        }
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

        String bodyText = sourceBuilder.getExpressionBodyText(output.get().value().toString(), null).orElse("");

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
                lineRange -> sourceBuilder.textEdit(false, "data_mappings.bal"),
                () -> sourceBuilder.textEdit(false, "data_mappings.bal"));

        return sourceBuilder.build();
    }
}
