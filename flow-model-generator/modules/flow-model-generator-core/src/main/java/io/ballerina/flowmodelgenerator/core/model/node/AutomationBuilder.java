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
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.model.types.TypeKind;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of an automation form.
 *
 * @since 2.0.0
 */
public class AutomationBuilder extends FunctionDefinitionBuilder {

    public static final String LABEL = "Automation";
    public static final String DESCRIPTION = "Define an automation";
    public static final String MAIN_FUNCTION_NAME = "main";

    public static final String PARAMETERS_LABEL = "Startup Parameters";
    public static final String PARAMETERS_DOC = "Define the parameters to be passed to the automation at startup";

    public static final String RETURN_ERROR_KEY = "returnError";
    public static final String RETURN_ERROR_LABEL = "Return Error";
    public static final String RETURN_ERROR_DOC = "Indicate if the automation should exit with error";

    private static final String BALLERINA_LOG_MODULE = "log";
    private static final String DEFAULT_BODY =
            "do {\n} on fail error e {\n  log:printError(\"Error occurred\", 'error=e);\n   return e;\n}";
    private static final List<String> TYPE_CONSTRAINT = List.of(
            TypeKind.STRING.typeName(),
            TypeKind.INT.typeName(),
            TypeKind.FLOAT.typeName(),
            TypeKind.DECIMAL.typeName(),
            TypeKind.BYTE.typeName()
    );
    private static final Gson gson = new Gson();

    public static Property getParameterSchema() {
        return ParameterSchemaHolder.PARAMETER_SCHEMA;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.AUTOMATION);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        sendMandatoryProperties(this);
        setOptionalProperties(this, true);
    }

    public static void sendMandatoryProperties(NodeBuilder nodeBuilder) {
        nodeBuilder.properties().custom()
                .metadata()
                    .label(FUNCTION_NAME_LABEL)
                    .description(FUNCTION_NAME_DOC)
                    .stepOut()
                .value(MAIN_FUNCTION_NAME)
                .type(Property.ValueType.IDENTIFIER)
                .typeConstraint(Property.GLOBAL_SCOPE)
                .hidden()
                .stepOut()
                .addProperty(Property.FUNCTION_NAME_KEY);
        nodeBuilder.properties().nestedProperty();
    }

    public static void setProperty(FormBuilder<?> formBuilder, String type, String name, Token token) {
        formBuilder.parameter(type, name, token, Property.ValueType.SINGLE_SELECT, TYPE_CONSTRAINT);
    }

    public static void setOptionalProperties(NodeBuilder nodeBuilder, boolean returnError) {
        nodeBuilder.properties()
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, Property.PARAMETERS_KEY, PARAMETERS_LABEL,
                        PARAMETERS_DOC, getParameterSchema(), true, true);
        nodeBuilder.properties().custom()
                .metadata()
                    .label(RETURN_ERROR_LABEL)
                    .description(RETURN_ERROR_DOC)
                    .stepOut()
                .value(returnError)
                .editable(true)
                .type(Property.ValueType.FLAG)
                .advanced(true)
                .stepOut()
                .addProperty(RETURN_ERROR_KEY);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.token()
                .keyword(SyntaxKind.PUBLIC_KEYWORD)
                .keyword(SyntaxKind.FUNCTION_KEYWORD)
                .name(MAIN_FUNCTION_NAME)
                .keyword(SyntaxKind.OPEN_PAREN_TOKEN);

        // Write the automation parameters
        Optional<Property> parameters = sourceBuilder.getProperty(Property.PARAMETERS_KEY);
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
        Optional<Property> returnError = sourceBuilder.getProperty(RETURN_ERROR_KEY);
        boolean hasReturnError = returnError.isPresent() && returnError.get().value().equals(true);
        if (hasReturnError) {
            sourceBuilder.token()
                    .keyword(SyntaxKind.RETURNS_KEYWORD)
                    .name("error?");
        }

        // Generate text edits based on the line range
        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (lineRange == null) {
            sourceBuilder.token().openBrace();
            if (hasReturnError) {
                sourceBuilder.token().name(DEFAULT_BODY);
                sourceBuilder.acceptImport(CommonUtils.BALLERINA_ORG_NAME, BALLERINA_LOG_MODULE);
            }
            sourceBuilder.token().closeBrace()
                    .stepOut()
                    .textEdit(SourceBuilder.SourceKind.DECLARATION);
        } else {
            sourceBuilder
                    .token().skipFormatting().stepOut()
                    .textEdit();
        }
        return sourceBuilder.build();
    }

    private static class ParameterSchemaHolder {

        private static final Property PARAMETER_SCHEMA = initParameterSchema();

        private static Property initParameterSchema() {
            FormBuilder<?> formBuilder = new FormBuilder<>(null, null, null, null);
            setProperty(formBuilder, "", "", null);
            formBuilder.parameter("", "", null, Property.ValueType.SINGLE_SELECT, TYPE_CONSTRAINT);
            Map<String, Property> nodeProperties = formBuilder.build();
            return nodeProperties.get("");
        }
    }
}
