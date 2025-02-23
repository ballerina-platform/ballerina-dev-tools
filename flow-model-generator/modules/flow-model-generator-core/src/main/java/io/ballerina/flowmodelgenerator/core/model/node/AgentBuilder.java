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

/**
 * Represents agent node in the flow model.
 *
 * @since 2.0.0
 */
public class AgentBuilder extends NodeBuilder {

    private static final String AGENT_LABEL = "Agent";
    public static final String INIT_SYMBOL = "init";
    public static final String CLIENT_SYMBOL = "Client";
    public static final String CHECK_ERROR_DOC = "Terminate on error";
    public static final String AGENT_NAME_LABEL = "Agent Name";
    protected static final Logger LOG = LoggerFactory.getLogger(AgentBuilder.class);

    @Override
    public void setConcreteConstData() {
        metadata().label(AGENT_LABEL);
        codedata().node(NodeKind.AGENT).symbol("init");
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
                .agentParameters(sourceBuilder.flowNode);

        return sourceBuilder.textEdit(false, "agents.bal", true).build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        FunctionResult function = getInitFunctionResult(codedata.object());
        List<ParameterResult> functionParameters = getInitFunctionParameterResults(codedata.object());

        metadata()
                .label(function.packageName())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .node(NodeKind.AGENT)
                .org(function.org())
                .module(function.packageName())
                .object(codedata.object())
                .symbol(INIT_SYMBOL)
                .id(function.functionId())
                .isGenerated(codedata.isGenerated());

        if (functionParameters.size() != 2) {
            throw new IllegalStateException(String.format("Invalid number of parameters in agent %s", function.name()));
        }

        createParameterProperty(functionParameters.getFirst(), Property.ValueType.EXPRESSION);
        createParameterProperty(functionParameters.get(1), Property.ValueType.EXPRESSION_SET);

        if (CommonUtils.hasReturn(function.returnType())) {
            properties()
                    .type(function.returnType(), false)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), AGENT_NAME_LABEL);
        }
        properties()
                .scope(Property.GLOBAL_SCOPE)
                .checkError(true, CHECK_ERROR_DOC, false);
    }

    private void createParameterProperty(ParameterResult paramResult, Property.ValueType valueType) {
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
                .defaultable(paramResult.optional())
                .type(valueType)
                .value(paramResult.defaultValue())
                .stepOut()
                .addProperty(unescapedParamName);
    }

    private FunctionResult getInitFunctionResult(String agentName) {
        if (agentName.equals("ReActAgent")) {
            return new FunctionResult(-1, "ReActAgent", "React agent", "error?", "ai.agent", "wso2", "1.0.0", "",
                    Function.Kind.FUNCTION, true, false);
        } else if (agentName.equals("FunctionCallAgent")) {
            return new FunctionResult(-1, "FunctionCallAgent", "Function call agent", "error?", "ai.agent", "wso2",
                    "1.0.0", "", Function.Kind.FUNCTION, true, false);
        }
        throw new IllegalStateException(String.format("Agent %s is not supported", agentName));
    }

    private List<ParameterResult> getInitFunctionParameterResults(String agentName) {
        if (agentName.equals("ReActAgent")) {
            return List.of(
                    new ParameterResult(-1, "model", "CompletionLlmModel|ChatLlmModel", Parameter.Kind.REQUIRED, "",
                            "", false, ""),
                    new ParameterResult(-2, "tools", "BaseToolKit|Tool", Parameter.Kind.REST_PARAMETER, "", "", false
                            , "")
            );
        } else if (agentName.equals("FunctionCallAgent")) {
            return List.of(
                    new ParameterResult(-1, "model", "FunctionCallLlmModel", Parameter.Kind.REQUIRED, "", "", false,
                            ""),
                    new ParameterResult(-2, "tools", "BaseToolKit|Tool", Parameter.Kind.REST_PARAMETER, "", "", false
                            , "")
            );
        }
        throw new IllegalStateException(String.format("Agent %s is not supported", agentName));
    }
}
