/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

import com.google.gson.Gson;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.RecordUtil;
import org.ballerinalang.model.types.TypeKind;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of a data mapper definition node.
 *
 * @since 2.0.0
 */
public class DataMapperDefinitionBuilder extends NodeBuilder {

    public static final String LABEL = "Data Mapper";
    public static final String DESCRIPTION = "Define a data mapper";

    public static final String DATA_MAPPER_NAME_LABEL = "Data Mapper Name";
    public static final String DATA_MAPPER_NAME_DOC = "Name of the data mapper";

    public static final String PARAMETERS_LABEL = "Inputs";
    public static final String PARAMETERS_DOC = "Input variables of the data mapper function";

    private static final String DATA_MAPPINGS_BAL = "data_mappings.bal";
    private static final Gson gson = new Gson();

    public static final String RECORD_TYPE = TypeKind.RECORD.typeName();

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.DATA_MAPPER_DEFINITION);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties()
                .functionNameTemplate("transform", context.getAllVisibleSymbolNames(),
                        DATA_MAPPER_NAME_LABEL, DATA_MAPPER_NAME_DOC)
                .returnType(null, RECORD_TYPE, false)
                .nestedProperty()
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, Property.PARAMETERS_KEY, PARAMETERS_LABEL,
                        PARAMETERS_DOC, FunctionDefinitionBuilder.getParameterSchema(), false);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.token().keyword(SyntaxKind.FUNCTION_KEYWORD);

        // Write the data mapper name
        Optional<Property> property = sourceBuilder.flowNode.getProperty(Property.FUNCTION_NAME_KEY);
        if (property.isEmpty()) {
            throw new IllegalStateException("Data mapper name is not present");
        }
        sourceBuilder.token()
                .name(property.get().value().toString())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        // Write the data mapper parameters
        Optional<Property> parameters = sourceBuilder.flowNode.getProperty(Property.PARAMETERS_KEY);
        if (parameters.isPresent() && parameters.get().value() instanceof Map<?, ?> paramMap) {
            List<String> paramList = new ArrayList<>();
            for (Object obj : paramMap.values()) {
                Property paramProperty = gson.fromJson(gson.toJsonTree(obj), Property.class);
                if (!(paramProperty.value() instanceof Map<?, ?> paramData)) {
                    continue;
                }
                Map<String, Property> paramProperties = gson.fromJson(gson.toJsonTree(paramData),
                        FormBuilder.NODE_PROPERTIES_TYPE);
                String paramType = paramProperties.get(Property.TYPE_KEY).value().toString();
                String paramName = paramProperties.get(Property.VARIABLE_KEY).value().toString();
                paramList.add(paramType + " " + paramName);
            }
            sourceBuilder.token().name(String.join(", ", paramList));
        }
        sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

        // Write the return type
        Optional<Property> returnType = sourceBuilder.flowNode.getProperty(Property.TYPE_KEY);
        if (returnType.isEmpty() || returnType.get().value().toString().isEmpty()) {
            throw new IllegalStateException("The data mapper should have an output");
        }
        String returnTypeString = returnType.get().value().toString();
        sourceBuilder.token()
                .keyword(SyntaxKind.RETURNS_KEYWORD)
                .name(returnTypeString);

        // Generate text edits based on the line range. If a line range exists, update the signature of the existing
        // function. Otherwise, create a new function definition in "data_mappings.bal".
        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (lineRange == null) {
            // The return type symbol should be present
            Optional<TypeDefinitionSymbol> returnTypeSymbol = sourceBuilder.getTypeDefinitionSymbol(returnTypeString);
            if (returnTypeSymbol.isEmpty()) {
                throw new IllegalStateException("Return type symbol not found: " + returnTypeString);
            }

            // The return type should a record type
            TypeSymbol recordTypeSymbol = returnTypeSymbol.get().typeDescriptor();
            if (recordTypeSymbol.typeKind() != TypeDescKind.RECORD) {
                throw new IllegalStateException("Return type should be a record type: " + returnTypeString);
            }

            // Generate the body text
            String bodyText = RecordUtil.getFillAllRecordFieldInsertText(
                    ((RecordTypeSymbol) recordTypeSymbol).fieldDescriptors());
            sourceBuilder
                    .token()
                        .keyword(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN)
                        .openBrace()
                        .name(bodyText)
                        .closeBrace()
                        .endOfStatement()
                        .stepOut()
                    .textEdit(false, DATA_MAPPINGS_BAL);
        } else {
            sourceBuilder
                    .token().skipFormatting().stepOut()
                    .textEdit(false);
        }
        return sourceBuilder.build();
    }
}
