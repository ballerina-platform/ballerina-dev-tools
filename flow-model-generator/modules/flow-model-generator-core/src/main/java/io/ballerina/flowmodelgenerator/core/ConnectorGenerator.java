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
import io.ballerina.flowmodelgenerator.core.central.ConnectorsResponse;
import io.ballerina.flowmodelgenerator.core.central.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.central.RemoteCentral;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnection;
import org.ballerinalang.central.client.model.Package;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the connectors for the provided parameters.
 *
 * @since 1.4.0
 */
public class ConnectorGenerator {

    private final Gson gson;

    public ConnectorGenerator() {
        this.gson = new Gson();
    }

    public JsonArray getConnectors(Map<String, String> queryMap) {
        // Get the popular connectors by default
        if (queryMap == null || queryMap.isEmpty() || !queryMap.containsKey("q") || queryMap.get("q").isEmpty()) {
            return gson.toJsonTree(LocalIndexCentral.getInstance().getConnectors()).getAsJsonArray();
        }

        // Filter the connectors published by ballerina and ballerinax
        Map<String, String> newQueryMap = new HashMap<>(queryMap);
        newQueryMap.put("org", "ballerina,ballerinax");

        // Get the connectors from the central
        ConnectorsResponse connectorsResponse = RemoteCentral.getInstance().connectors(newQueryMap);
        List<AvailableNode> connectors = connectorsResponse.connectors().stream()
                .filter(connector -> connector.name.equals(NewConnection.CLIENT_SYMBOL))
                .map(connector -> {
                    Package packageInfo = connector.packageInfo;
                    Metadata metadata = new Metadata.Builder<>(null)
                            .label(connector.moduleName)
                            .description(packageInfo.getSummary())
                            .keywords(packageInfo.getKeywords())
                            .icon(connector.icon).build();
                    Codedata codedata = new Codedata.Builder<>(null)
                            .node(NodeKind.NEW_CONNECTION)
                            .org(packageInfo.getOrganization())
                            .module(packageInfo.getName())
                            .object(connector.name)
                            .symbol(NewConnection.INIT_SYMBOL)
                            .id(connector.id)
                            .build();
                    return new AvailableNode(metadata, codedata, true);
                }).toList();
        return gson.toJsonTree(connectors).getAsJsonArray();

    }
}
