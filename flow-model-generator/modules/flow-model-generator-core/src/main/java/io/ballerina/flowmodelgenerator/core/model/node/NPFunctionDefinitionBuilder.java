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
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.ballerina.modelgenerator.commons.ParameterData.Kind.DEFAULTABLE;
import static io.ballerina.modelgenerator.commons.ParameterData.Kind.REQUIRED;

/**
* Represents the properties of a Natural programming function definition node.
 *
 * @since 2.0.0
 */
public class NPFunctionDefinitionBuilder extends FunctionDefinitionBuilder {
    public static final String LABEL = "Prompt as a code";
    public static final String DESCRIPTION = "Define a prompt as a code";

    public static final String PROMPT_AS_A_CODE_NAME_DESCRIPTION = "Name for the Prompt as a code";
    public static final String PROMPT_AS_A_CODE_NAME_LABEL = "Name";

    public static final String PARAMETERS_LABEL = "Parameters";
    public static final String PARAMETERS_DOC = "Prompt parameters";

    private static final String PROMPT = "prompt";
    private static final String PROMPT_LABEL = "Prompt";
    private static final String PROMPT_DESCRIPTION = "Prompt for the function";
    private static final String PROMPT_TYPE = "np:Prompt";

    private static final String MODEL = "model";
    private static final String MODEL_LABEL = "Model";
    private static final String MODEL_DESCRIPTION = "Model to be used";
    private static final String MODEL_TYPE = "np:Model";

    private static final String FUNCTIONS_BAL = "functions.bal";

    private static final String BALLERINAX_ORG = "ballerinax";
    private static final String NP_PACKAGE = "np";

    private static final Gson gson = new Gson();

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata()
                .node(NodeKind.FUNCTION_DEFINITION)
                .org(BALLERINAX_ORG)
                .module(NP_PACKAGE);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties()
                .functionNameTemplate("promptAsCode",
                        context.getAllVisibleSymbolNames(),
                        PROMPT_AS_A_CODE_NAME_LABEL,
                        PROMPT_AS_A_CODE_NAME_DESCRIPTION)
                .returnType(null)
                .nestedProperty()
                .endNestedProperty(Property.ValueType.REPEATABLE_PROPERTY, Property.PARAMETERS_KEY, PARAMETERS_LABEL,
                        PARAMETERS_DOC, getParameterSchema(), true);

        // prompt
        properties().custom()
                    .metadata()
                        .label(PROMPT_LABEL)
                        .description(PROMPT_DESCRIPTION)
                        .stepOut()
                    .codedata()
                        .kind(REQUIRED.name())
                        .stepOut()
                    .placeholder("")
                    .typeConstraint(PROMPT_TYPE)
                    .editable()
                    .type(Property.ValueType.RAW_TEMPLATE)
                    .stepOut()
                    .addProperty(PROMPT);

        // model
        properties().custom()
                    .metadata()
                        .label(MODEL_LABEL)
                        .description(MODEL_DESCRIPTION)
                        .stepOut()
                    .codedata()
                        .kind(DEFAULTABLE.name())
                        .stepOut()
                    .placeholder("")
                    .typeConstraint(MODEL_TYPE)
                    .editable()
                    .optional(true)
                    .advanced(true)
                    .type(Property.ValueType.EXPRESSION)
                    .stepOut()
                    .addProperty(MODEL);
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

        // Write the function parameters
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
            if (!paramList.isEmpty()) {
                sourceBuilder.token().keyword(SyntaxKind.COMMA_TOKEN);
            }
        }

        // Write prompt parameter
        Optional<Property> promptProperty = sourceBuilder.flowNode.getProperty(PROMPT);
        String defaultValue = promptProperty.map(value -> " = " + value.value().toString()).orElse("");
        sourceBuilder.token().name(PROMPT_TYPE + " " + PROMPT + defaultValue);

        sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

        // Write the return type
        Optional<Property> returnType = sourceBuilder.flowNode.getProperty(Property.TYPE_KEY);
        if (returnType.isPresent() && !returnType.get().value().toString().isEmpty()) {
            sourceBuilder.token()
                    .keyword(SyntaxKind.RETURNS_KEYWORD)
                    .name(returnType.get().value() + "|error");
        } else {
            sourceBuilder.token().keyword(SyntaxKind.RETURNS_KEYWORD).name("error");
        }

        // Generate text edits based on the line range. If a line range exists, update the signature of the existing
        // function. Otherwise, create a new function definition in "functions.bal".
        LineRange lineRange = sourceBuilder.flowNode.codedata().lineRange();
        if (lineRange == null) {
            sourceBuilder
                    .token()
                        .equal()
                        .name("@np:LlmCall external")
                        .semicolon()
                        .skipFormatting()
                        .stepOut()
                    .textEdit(false, FUNCTIONS_BAL, false);
        } else {
            sourceBuilder
                    .token().skipFormatting().stepOut()
                    .textEdit(false);
        }
        return sourceBuilder.build();
    }
}
