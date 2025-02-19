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

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.db.model.Function;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import org.eclipse.lsp4j.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents class initialization node in the flow model.
 *
 * @since 2.0.0
 */
public class ClassBuilder extends NodeBuilder {

    private static final String CLASS_LABEL = "Class";

    public static final String INIT_SYMBOL = "init";
    public static final String CHECK_ERROR_DOC = "Terminate on error";
    public static final String CLASS_NAME_LABEL = "Class Name";
    protected static final Logger LOG = LoggerFactory.getLogger(ClassBuilder.class);

    @Override
    public void setConcreteConstData() {
        metadata().label(CLASS_LABEL);
        codedata().node(NodeKind.CLASS).symbol("init");
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder
                .token().keyword(SyntaxKind.FINAL_KEYWORD).stepOut()
                .newVariable();

        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(sourceBuilder.flowNode,
                        Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY, Property.SCOPE_KEY,
                                Property.CHECK_ERROR_KEY));

        return sourceBuilder.textEdit(false, "agents.bal", true).build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        FunctionResult function = getInitFunctionResult(codedata.object());
        List<ParameterResult> functionParameters = getInitFunctionParameters(codedata.object());

        metadata()
                .label(function.packageName())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .node(NodeKind.CLASS)
                .org(function.org())
                .module(function.packageName())
                .object(codedata.object())
                .symbol(INIT_SYMBOL)
                .id(function.functionId())
                .isGenerated(codedata.isGenerated());

        boolean hasOnlyRestParams = functionParameters.size() == 1;
        for (ParameterResult paramResult : functionParameters) {
            if (paramResult.kind().equals(Parameter.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(Parameter.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .importStatements(paramResult.importStatements())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional());

            if (paramResult.kind() == Parameter.Kind.INCLUDED_RECORD_REST) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                unescapedParamName = "additionalValues";
                customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REST_PARAMETER) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REQUIRED) {
                customPropBuilder.type(Property.ValueType.EXPRESSION).value(paramResult.defaultValue());
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }

        if (CommonUtils.hasReturn(function.returnType())) {
            properties()
                    .type(function.returnType(), false)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), CLASS_NAME_LABEL);
        }
        properties()
                .scope(Property.GLOBAL_SCOPE)
                .checkError(true, CHECK_ERROR_DOC, false);
    }

    private FunctionResult getInitFunctionResult(String name) {
        if (name.equals("ChatGptModel")) {
            return new FunctionResult(-1, "ChatGptModel", "ChatGPT model", "error?", "ballerina", "agent", "1.0.0", "",
                    Function.Kind.FUNCTION, true, false);
        } else if (name.equals("AzureChatGptModel")) {
            return new FunctionResult(-1, "AzureChatGptModel", "ChatGPT model", "error?", "ballerina", "agent",
                    "1.0.0", "", Function.Kind.FUNCTION, true, false);
        }
        throw new IllegalStateException(String.format("Agent %s is not supported", name));
    }

    private List<ParameterResult> getInitFunctionParameters(String name) {
        if (name.equals("ChatGptModel")) {
            return List.of(
                    new ParameterResult(-1, "connectionConfig", "chat:ConnectionConfig", Parameter.Kind.REQUIRED, "",
                            "", false, ""),
                    new ParameterResult(-2, "modelConfig", "ChatModelConfig", Parameter.Kind.DEFAULTABLE, "{}", "",
                            false
                            , "")
            );
        } else if (name.equals("AzureChatGptModel")) {
            return List.of(
                    new ParameterResult(-1, "connectionConfig", "azure_chat:ConnectionConfig",
                            Parameter.Kind.REQUIRED, "", "", false,
                            ""),
                    new ParameterResult(-2, "serviceUrl", "string", Parameter.Kind.REQUIRED, "", "", false,
                            ""),
                    new ParameterResult(-3, "deploymentId", "string", Parameter.Kind.REQUIRED, "", "", false,
                            ""),
                    new ParameterResult(-4, "apiVersion", "string", Parameter.Kind.REQUIRED, "", "", false,
                            ""),
                    new ParameterResult(-5, "modelConfig", "ChatModelConfig", Parameter.Kind.DEFAULTABLE, "{}", "",
                            false
                            , "")
            );
        }
        throw new IllegalStateException(String.format("Class %s is not supported", name));
    }
}
