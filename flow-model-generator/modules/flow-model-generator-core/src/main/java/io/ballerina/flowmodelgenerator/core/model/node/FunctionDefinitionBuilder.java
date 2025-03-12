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
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.model.types.TypeKind;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of a function definition node.
 *
 * @since 2.0.0
 */
public class FunctionDefinitionBuilder extends NodeBuilder {

    public static final String LABEL = "Function Definition";
    public static final String DESCRIPTION = "Define a function";

    public static final String FUNCTION_NAME_LABEL = "Name";
    public static final String FUNCTION_NAME_DOC = "Name of the function";

    public static final String PARAMETERS_LABEL = "Parameters";
    public static final String PARAMETERS_DOC = "Function parameters";

    private static final String FUNCTIONS_BAL = "functions.bal";

    private static final Gson gson = new Gson();

    public static Property getParameterSchema() {
        return ParameterSchemaHolder.PARAMETER_SCHEMA;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.FUNCTION_DEFINITION);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().functionNameTemplate("function", context.getAllVisibleSymbolNames());
        setProperties(this, null);
        endNestedProperties(this);
    }

    public static void setProperties(NodeBuilder nodeBuilder, String returnType) {
        nodeBuilder.properties()
                .returnType(returnType, null, true)
                .nestedProperty();
    }

    public static void setProperty(FormBuilder<?> formBuilder, String type, String name, Token token) {
        formBuilder.parameter(type, name, token, Property.ValueType.TYPE, null);
    }

    public static void endNestedProperties(NodeBuilder nodeBuilder) {
        nodeBuilder.properties()
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, Property.PARAMETERS_KEY, PARAMETERS_LABEL,
                        PARAMETERS_DOC, getParameterSchema(), true, false);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.token().keyword(SyntaxKind.FUNCTION_KEYWORD);

        // Write the function name
        Optional<Property> property = sourceBuilder.flowNode.getProperty(Property.FUNCTION_NAME_KEY);
        if (property.isEmpty()) {
            throw new IllegalStateException("Function name is not present");
        }
        sourceBuilder.token()
                .name(property.get().value().toString())
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        // WRite the function parameters
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
        if (returnType.isPresent() && !returnType.get().value().toString().isEmpty()) {
            sourceBuilder.token()
                    .keyword(SyntaxKind.RETURNS_KEYWORD)
                    .name(returnType.get().value().toString());
        }

        // Generate text edits based on the line range. If a line range exists, update the signature of the existing
        // function. Otherwise, create a new function definition in "functions.bal".
        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (lineRange == null) {
            sourceBuilder
                    .token()
                        .openBrace()
                        .closeBrace()
                        .stepOut()
                    .textEdit(false, FUNCTIONS_BAL);
        } else {
            sourceBuilder
                    .token().skipFormatting().stepOut()
                    .textEdit(false);
        }
        return sourceBuilder.build();
    }

    private static class ParameterSchemaHolder {

        private static final Property PARAMETER_SCHEMA = initParameterSchema();

        private static Property initParameterSchema() {
            FormBuilder<?> formBuilder = new FormBuilder<>(null, null, null, null);
            setProperty(formBuilder, "", "", null);
            Map<String, Property> nodeProperties = formBuilder.build();
            return nodeProperties.get("");
        }
    }
}
