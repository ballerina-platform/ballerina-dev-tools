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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for generating types from the semantic model.
 */
public class AgentsGenerator {

    private final Gson gson;
    private final Map<String, Set<String>> modelsForAgent = Map.of("FunctionCallAgent", Set.of("ChatGptModel", "AzureChatGptModel"), "ReActAgent", Set.of("ChatGptModel", "AzureChatGptModel"));

    public AgentsGenerator() {
        this.gson = new Gson();
    }

    public JsonArray getAgents() {
        Codedata.Builder<Object> codedataBuilder = new Codedata.Builder<>(null);
        Codedata functionCallAgent = codedataBuilder.node(NodeKind.AGENT)
                .org("wso2")
                .module("ai.agent")
                .object("FunctionCallAgent")
                .symbol("init")
                .build();
        Codedata reactCallAgent = codedataBuilder.node(NodeKind.AGENT)
                .org("wso2")
                .module("ai.agent")
                .object("ReActAgent")
                .symbol("init")
                .build();
        List<Codedata> agents = List.of(functionCallAgent, reactCallAgent);
        return gson.toJsonTree(agents).getAsJsonArray();
    }

    public JsonArray getAllModels(String agent) {
        Codedata.Builder<Object> codedataBuilder = new Codedata.Builder<>(null);
        Codedata chatGptModel = codedataBuilder.node(NodeKind.CLASS)
                .org("wso2")
                .module("ai.model")
                .object("ChatGptModel")
                .symbol("init")
                .build();
        Codedata azureChatGptModel = codedataBuilder.node(NodeKind.CLASS)
                .org("wso2")
                .module("ai.model")
                .object("AzureChatGptModel")
                .symbol("init")
                .build();
        if (agent.equals("FunctionCallAgent")) {
            List<Codedata> models = List.of(chatGptModel, azureChatGptModel);
            return gson.toJsonTree(models).getAsJsonArray();
        } else if (agent.equals("ReActAgent")) {
            List<Codedata> models = List.of(chatGptModel, azureChatGptModel);
            return gson.toJsonTree(models).getAsJsonArray();
        }
        throw new IllegalStateException(String.format("Agent %s is not supported", agent));
    }

    public JsonArray getModels(SemanticModel semanticModel, String agent) {
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        Set<String> models = modelsForAgent.get(agent);
        if (models == null) {
            throw new IllegalStateException(String.format("Cannot find models for agent %s", agent));
        }
        List<String> availableModels = new ArrayList<>();
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() != SymbolKind.VARIABLE) {
                continue;
            }
            VariableSymbol variableSymbol = (VariableSymbol) moduleSymbol;
            TypeSymbol typeSymbol = variableSymbol.typeDescriptor();
            String signature = typeSymbol.signature();
            if (models.contains(signature)) {
                availableModels.add(signature);
            }
        }
        return gson.toJsonTree(availableModels).getAsJsonArray();
    }

    public JsonArray getTools(SemanticModel semanticModel) {
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        List<String> functionNames = new ArrayList<>();
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() == SymbolKind.FUNCTION) {
                functionNames.add((moduleSymbol).getName().orElse(""));
            }
        }
        return gson.toJsonTree(functionNames).getAsJsonArray();
    }
}
