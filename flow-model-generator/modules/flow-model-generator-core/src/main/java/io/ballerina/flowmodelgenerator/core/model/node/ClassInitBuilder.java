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
    public static final String OPEN_AI_MODEL = "OpenAiProvider";
    public static final List<String> OPEN_AI_MODEL_TYPES = List.of(
            "ai:O3_MINI",
            "ai:O3_MINI_2025_01_31",
            "ai:O1",
            "ai:O1_2024_12_17",
            "ai:GPT_4O",
            "ai:GPT_4O_2024_11_20",
            "ai:GPT_4O_2024_08_06",
            "ai:GPT_4O_2024_05_13",
            "ai:GPT_4O_MINI",
            "ai:GPT_4O_MINI_2024_07_18",
            "ai:GPT_4_TURBO",
            "ai:GPT_4_TURBO_2024_04_09",
            "ai:GPT_4_0125_PREVIEW",
            "ai:GPT_4_TURBO_PREVIEW",
            "ai:GPT_4_1106_PREVIEW",
            "ai:GPT_4_VISION_PREVIEW",
            "ai:GPT_4",
            "ai:GPT_4_0314",
            "ai:GPT_4_0613",
            "ai:GPT_4_32K",
            "ai:GPT_4_32K_0314",
            "ai:GPT_4_32K_0613",
            "ai:GPT_3_5_TURBO",
            "ai:GPT_3_5_TURBO_16K",
            "ai:GPT_3_5_TURBO_0301",
            "ai:GPT_3_5_TURBO_0613",
            "ai:GPT_3_5_TURBO_1106",
            "ai:GPT_3_5_TURBO_0125",
            "ai:GPT_3_5_TURBO_16K_0613"
    );

    public static final String ANTHROPIC_MODEL = "AnthropicProvider";
    public static final List<String> ANTHROPIC_MODEL_TYPES = List.of(
            "ai:CLAUDE_3_7_SONNET_20250219",
            "ai:CLAUDE_3_5_HAIKU_20241022",
            "ai:CLAUDE_3_5_SONNET_20241022",
            "ai:CLAUDE_3_5_SONNET_20240620",
            "ai:CLAUDE_3_OPUS_20240229",
            "ai:CLAUDE_3_SONNET_20240229",
            "ai:CLAUDE_3_HAIKU_20240307"
    );

    public static final String MISTRAL_AI_MODEL = "MistralAiModel";

    public static final List<String> MISTRAL_AI_MODEL_TYPES = List.of(
            "ai:MINISTRAL_3B_2410",
            "ai:MINISTRAL_8B_2410",
            "ai:OPEN_MISTRAL_7B ",
            "ai:OPEN_MISTRAL_NEMO",
            "ai:OPEN_MIXTRAL_8X7B",
            "ai:OPEN_MIXTRAL_8X22B",
            "ai:MISTRAL_SMALL_2402",
            "ai:MISTRAL_SMALL_2409",
            "ai:MISTRAL_SMALL_2501",
            "ai:MISTRAL_MEDIUM_2312",
            "ai:MISTRAL_LARGE_2402",
            "ai:MISTRAL_LARGE_2407",
            "ai:MISTRAL_LARGE_2411",
            "ai:PIXTRAL_LARGE_2411",
            "ai:CODESTRAL_2405",
            "ai:CODESTRAL_2501",
            "ai:CODESTRAL_MAMBA_2407",
            "ai:PIXTRAL_12B_2409",
            "ai:MISTRAL_SABA_2502 ",
            "ai:MISTRAL_SMALL_MODEL ",
            "ai:MISTRAL_MEDIUM_MODEL",
            "ai:MISTRAL_LARGE_MODEL"
    );

    public static final String MODEL_TYPE = "modelType";
    public static final String REQUIRED = "REQUIRED";
    public static final String BALLERINAX = "ballerinax";
    public static final String AI_AGENT = "ai";
    public static final String OPEN_AI_MODEL_DESC = "The OpenAI model name as constant from OPEN_AI_MODEL_NAMES enum";
    public static final String ANTHROPIC_MODEL_DESC =
            "The Anthropic model name as constant from ANTHROPIC_MODEL_NAMES enum";
    public static final String MISTRAL_AI_MODEL_DESC =
            "The Mistral AI model name as constant from MISTRAL_AI_MODEL_NAMES enum";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.CLASS_INIT;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return FunctionData.Kind.CLASS_INIT;
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

        return sourceBuilder.textEdit().acceptImport().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        super.setConcreteTemplateData(context);

        Codedata codedata = context.codedata();

        if (!codedata.org().equals(BALLERINAX) || !codedata.module().equals(AI_AGENT)) {
            return;
        }
        switch (codedata.object()) {
            case OPEN_AI_MODEL -> setAIModelType(OPEN_AI_MODEL_TYPES,
                    OPEN_AI_MODEL_DESC, "ai:OPEN_AI_MODEL_NAMES", "\"gpt-3.5-turbo-16k-0613\"", codedata);
            case ANTHROPIC_MODEL -> setAIModelType(ANTHROPIC_MODEL_TYPES,
                    ANTHROPIC_MODEL_DESC, "ai:ANTHROPIC_MODEL_NAMES", "\"claude-3-haiku-20240307\"", codedata);
            case MISTRAL_AI_MODEL -> setAIModelType(MISTRAL_AI_MODEL_TYPES,
                    MISTRAL_AI_MODEL_DESC, "ai:MISTRAL_AI_MODEL_NAMES", "\"mistral-large-latest\"", codedata);
            default -> {
                return;
            }
        }
    }

    private void setAIModelType(List<String> modelType, String description, String kind, String defaultValue,
                                Codedata codedata) {
        properties()
                .custom()
                .metadata()
                .label("Model Type")
                .description(description)
                .stepOut()
                .type(Property.ValueType.SINGLE_SELECT)
                .typeConstraint(modelType)
                .placeholder(defaultValue)
                .editable()
                .codedata()
                .kind(REQUIRED)
                .originalName(MODEL_TYPE)
                .stepOut()
                .typeMembers(List.of(new ParameterMemberTypeData(kind, "BASIC_TYPE",
                        codedata.org() + ":" + codedata.module() + ":" + codedata.version())))
                .stepOut()
                .addProperty(MODEL_TYPE);
    }
}
