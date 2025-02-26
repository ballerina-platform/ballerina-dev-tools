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

package io.ballerina.flowmodelgenerator.core.search;

import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.core.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.SearchResult;
import io.ballerina.projects.Module;
import io.ballerina.tools.text.LineRange;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the search command for connectors.
 *
 * @since 2.0.0
 */
public class ConnectorSearchCommand extends SearchCommand {

    private static final String CONNECTORS_LANDING_JSON = "connectors_landing.json";
    private static final Type CONNECTION_CATEGORY_LIST_TYPE = new TypeToken<Map<String, List<String>>>() { }.getType();

    public ConnectorSearchCommand(Module module, LineRange position, Map<String, String> queryMap) {
        super(module, position, queryMap);
    }

    @Override
    protected List<Item> defaultView() {
        Map<String, List<SearchResult>> categories = fetchPopularItems();
        for (Map.Entry<String, List<SearchResult>> entry : categories.entrySet()) {
            Category.Builder categoryBuilder = rootBuilder.stepIn(entry.getKey(), null, null);
            for (SearchResult searchResult : entry.getValue()) {
                SearchResult.Package packageInfo = searchResult.packageInfo();
                Metadata metadata = new Metadata.Builder<>(null)
                        .label(packageInfo.name().substring(0, 1).toUpperCase() + packageInfo.name().substring(1) +
                                " " + searchResult.name())
                        .description(searchResult.description())
                        .icon(CommonUtils.generateIcon(packageInfo.org(),
                                packageInfo.name(),
                                packageInfo.version()))
                        .build();
                Codedata codedata = new Codedata.Builder<>(null)
                        .node(NodeKind.NEW_CONNECTION)
                        .org(packageInfo.org())
                        .module(packageInfo.name())
                        .object(searchResult.name())
                        .symbol(NewConnectionBuilder.INIT_SYMBOL)
                        .version(packageInfo.version())
                        .build();
                categoryBuilder.node(new AvailableNode(metadata, codedata, true));
            }
        }
        return rootBuilder.build().items();
    }

    @Override
    protected List<Item> search() {
        List<SearchResult> searchResults = dbManager.searchConnectors(query, limit, offset);
        List<Item> connectors = new ArrayList<>();
        for (SearchResult connectorResult : searchResults) {
            SearchResult.Package packageInfo = connectorResult.packageInfo();

            Metadata metadata = new Metadata.Builder<>(null)
                    .label(packageInfo.name().substring(0, 1).toUpperCase() + packageInfo.name().substring(1) + " " +
                            connectorResult.name())
                    .description(connectorResult.description())
                    .icon(CommonUtils.generateIcon(packageInfo.org(),
                            packageInfo.name(),
                            packageInfo.version()))
                    .build();
            Codedata codedata = new Codedata.Builder<>(null)
                    .node(NodeKind.NEW_CONNECTION)
                    .org(packageInfo.org())
                    .module(packageInfo.name())
                    .object(connectorResult.name())
                    .symbol(NewConnectionBuilder.INIT_SYMBOL)
                    .version(packageInfo.version())
                    .build();
            connectors.add(new AvailableNode(metadata, codedata, true));
        }
        return connectors;
    }

    @Override
    protected Map<String, List<SearchResult>> fetchPopularItems() {
        Map<String, List<String>> categories = LocalIndexCentral.getInstance()
                .readJsonResource(CONNECTORS_LANDING_JSON, CONNECTION_CATEGORY_LIST_TYPE);

        Map<String, List<SearchResult>> defaultView = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> category : categories.entrySet()) {
            List<SearchResult> searchResults = dbManager.searchConnectorsByPackage(category.getValue(), limit, offset);
            defaultView.put(category.getKey(), searchResults);
        }
        return defaultView;
    }

}
