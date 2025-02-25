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
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DatabaseManager;
import io.ballerina.modelgenerator.commons.FunctionData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the connectors for the provided parameters.
 *
 * @since 2.0.0
 */
public class ConnectorGenerator {

    private final Gson gson;

    private static final String DEFAULT_LIMIT = "30";
    private static final String DEFAULT_OFFSET = "0";

    public ConnectorGenerator() {
        this.gson = new Gson();
    }

    public JsonArray getConnectors(Map<String, String> queryMap) {
        Map<String, String> modifiedQueryMap = new HashMap<>(queryMap);
        if (CommonUtils.hasNoKeyword(queryMap, "limit")) {
            modifiedQueryMap.put("limit", DEFAULT_LIMIT);
        }
        if (CommonUtils.hasNoKeyword(queryMap, "offset")) {
            modifiedQueryMap.put("offset", DEFAULT_OFFSET);
        }
        DatabaseManager dbManager = DatabaseManager.getInstance();

        List<FunctionData> connectorResults = CommonUtils.hasNoKeyword(queryMap, "q") ?
                dbManager.getAllFunctions(FunctionData.Kind.CONNECTOR, modifiedQueryMap) :
                dbManager.searchFunctions(modifiedQueryMap, FunctionData.Kind.CONNECTOR);

        List<AvailableNode> connectors = new ArrayList<>();
        for (FunctionData connectorResult : connectorResults) {
            Metadata metadata = new Metadata.Builder<>(null)
                    .label(connectorResult.packageName())
                    .description(connectorResult.description())
                    .icon(CommonUtils.generateIcon(connectorResult.org(), connectorResult.packageName(),
                            connectorResult.version()))
                    .build();
            Codedata codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.NEW_CONNECTION)
                    .org(connectorResult.org())
                    .module(connectorResult.packageName())
                    .object(NewConnectionBuilder.CLIENT_SYMBOL)
                    .symbol(NewConnectionBuilder.INIT_SYMBOL)
                    .id(connectorResult.functionId())
                    .build();
            connectors.add(new AvailableNode(metadata, codedata, true));
        }
        return gson.toJsonTree(connectors).getAsJsonArray();
    }
}
