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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;

import java.util.List;

/**
 * This class is responsible for generating types from the semantic model.
 */
public class AgentsGenerator {

    private static final List<String> DEFAULT_TYPES =
            List.of("int", "string", "float", "boolean", "decimal", "xml", "error", "function", "future", "typedesc",
                    "handle", "stream", "any", "anydata", "stream", "never", "readonly", "json", "byte");
    private final Gson gson;

    public AgentsGenerator() {
        this.gson = new Gson();
    }

    public JsonArray getAgents() {
        Codedata.Builder<Object> codedataBuilder = new Codedata.Builder<>(null);
        Codedata functionCallAgent = codedataBuilder.node(NodeKind.AGENT)
                .org("wso2")
                .module("ai.agent")
                .object("class")
                .symbol("FunctionCallAgent")
                .build();
        Codedata reactCallAgent = codedataBuilder.node(NodeKind.AGENT)
                .org("wso2")
                .module("ai.agent")
                .object("class")
                .symbol("ReActAgent")
                .build();
        List<Codedata> agents = List.of(functionCallAgent, reactCallAgent);
        return gson.toJsonTree(agents).getAsJsonArray();
    }

    public JsonArray getModels() {
        Codedata.Builder<Object> codedataBuilder = new Codedata.Builder<>(null);
        Codedata chatGptModel = codedataBuilder.node(NodeKind.CLASS)
                .org("wso2")
                .module("ai.model")
                .object("class")
                .symbol("ChatGptModel")
                .build();
        Codedata azureChatGptModel = codedataBuilder.node(NodeKind.CLASS)
                .org("wso2")
                .module("ai.model")
                .object("class")
                .symbol("AzureChatGptModel")
                .build();
        List<Codedata> models = List.of(chatGptModel, azureChatGptModel);
        return gson.toJsonTree(models).getAsJsonArray();
    }
}
