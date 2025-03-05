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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles the search command for connectors.
 *
 * @since 2.0.0
 */
public class ConnectorSearchCommand extends SearchCommand {

    private static final String CONNECTORS_LANDING_JSON = "connectors_landing.json";
    private static final Type CONNECTION_CATEGORY_LIST_TYPE = new TypeToken<Map<String, List<String>>>() { }.getType();

    // TODO: Remove this once the name is retrieved from the library module
    private static final String CONNECTOR_NAME_CORRECTION_JSON = "connector_name_correction.json";
    private static final Type CONNECTOR_NAME_MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();
    private static final Map<String, String> CONNECTOR_NAME_MAP =
            LocalIndexCentral.getInstance().readJsonResource(CONNECTOR_NAME_CORRECTION_JSON, CONNECTOR_NAME_MAP_TYPE);

    public ConnectorSearchCommand(Module module, LineRange position, Map<String, String> queryMap) {
        super(module, position, queryMap);
    }

    @Override
    protected List<Item> defaultView() {
        Map<String, List<SearchResult>> categories = fetchPopularItems();
        for (Map.Entry<String, List<SearchResult>> entry : categories.entrySet()) {
            Category.Builder categoryBuilder = rootBuilder.stepIn(entry.getKey(), null, null);
            entry.getValue().forEach(searchResult -> categoryBuilder.node(generateAvailableNode(searchResult)));
        }
        return rootBuilder.build().items();
    }

    @Override
    protected List<Item> search() {
        List<SearchResult> searchResults = dbManager.searchConnectors(query, limit, offset);
        searchResults.forEach(searchResult -> rootBuilder.node(generateAvailableNode(searchResult)));
        return rootBuilder.build().items();
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

    private static AvailableNode generateAvailableNode(SearchResult searchResult) {
        SearchResult.Package packageInfo = searchResult.packageInfo();
        Metadata metadata = new Metadata.Builder<>(null)
                .label(getConnectorName(searchResult, packageInfo))
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
        return new AvailableNode(metadata, codedata, true);
    }

    private static String getConnectorName(SearchResult searchResult, SearchResult.Package packageInfo) {
        String connectorName = searchResult.name();
        String rawPackageName = packageInfo.name();
        String packageName = CONNECTOR_NAME_MAP.getOrDefault(rawPackageName, getLastPackagePrefix(rawPackageName));
        return packageName + " " + connectorName;
    }

    private static String getLastPackagePrefix(String rawPackageName) {
        String trimmedPackageName = rawPackageName.contains(".")
                ? rawPackageName.substring(rawPackageName.lastIndexOf('.') + 1) : rawPackageName;
        return trimmedPackageName.substring(0, 1).toUpperCase(Locale.ROOT) + trimmedPackageName.substring(1);
    }
}
