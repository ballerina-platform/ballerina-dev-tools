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
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.ParameterMemberTypeData;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents class initialization node in the flow model.
 *
 * @since 2.0.0
 */
public class ClassInitBuilder extends CallBuilder {
    private static final String CLASS_LABEL = "Class";
    private static final String DEFAULT_MODEL_NAME = "model";
    private static final List<String> OPEN_AI_MODEL_TYPES = List.of(
            "agent:O3_MINI",
            "agent:O3_MINI_2025_01_31",
            "agent:O1",
            "agent:O1_2024_12_17",
            "agent:GPT_4O",
            "agent:GPT_4O_2024_11_20",
            "agent:GPT_4O_2024_08_06",
            "agent:GPT_4O_2024_05_13",
            "agent:GPT_4O_MINI",
            "agent:GPT_4O_MINI_2024_07_18",
            "agent:GPT_4_TURBO",
            "agent:GPT_4_TURBO_2024_04_09",
            "agent:GPT_4_0125_PREVIEW",
            "agent:GPT_4_TURBO_PREVIEW",
            "agent:GPT_4_1106_PREVIEW",
            "agent:GPT_4_VISION_PREVIEW",
            "agent:GPT_4",
            "agent:GPT_4_0314",
            "agent:GPT_4_0613",
            "agent:GPT_4_32K",
            "agent:GPT_4_32K_0314",
            "agent:GPT_4_32K_0613",
            "agent:GPT_3_5_TURBO",
            "agent:GPT_3_5_TURBO_16K",
            "agent:GPT_3_5_TURBO_0301",
            "agent:GPT_3_5_TURBO_0613",
            "agent:GPT_3_5_TURBO_1106",
            "agent:GPT_3_5_TURBO_0125",
            "agent:GPT_3_5_TURBO_16K_0613"
    );
    public static final String MODEL_TYPE = "modelType";
    public static final String REQUIRED = "REQUIRED";
    public static final String BALLERINAX = "ballerinax";
    public static final String AI_AGENT = "ai.agent";
    public static final String OPEN_AI_MODEL = "OpenAiModel";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.CLASS_INIT;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return FunctionData.Kind.CONNECTOR;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(CLASS_LABEL);
        codedata().node(NodeKind.CLASS_INIT).symbol("init");
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

        return sourceBuilder.textEdit(false, "agents.bal").build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        super.setConcreteTemplateData(context);

        Codedata codedata = context.codedata();
        if (codedata.org().equals(BALLERINAX) && codedata.module().equals(AI_AGENT) && codedata.object().equals(
                OPEN_AI_MODEL)) {
            properties()
                    .custom()
                    .metadata()
                    .label("Model Type")
                    .description("The OpenAI model name as constant from OPEN_AI_MODEL_NAMES enum")
                    .stepOut()
                    .type(Property.ValueType.SINGLE_SELECT)
                    .typeConstraint(OPEN_AI_MODEL_TYPES)
                    .placeholder("\"gpt-3.5-turbo-16k-0613\"")
                    .editable()
                    .codedata()
                    .kind(REQUIRED)
                    .originalName(MODEL_TYPE)
                    .stepOut()
                    .typeMembers(List.of(new ParameterMemberTypeData("agent:OPEN_AI_MODEL_NAMES", "BASIC_TYPE",
                            codedata.org() + ":" + codedata.module() + ":" + codedata.version())))
                    .stepOut()
                    .addProperty(MODEL_TYPE);
        }
    }
}
