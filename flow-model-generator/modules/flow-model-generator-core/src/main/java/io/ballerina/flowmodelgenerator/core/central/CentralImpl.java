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

package io.ballerina.flowmodelgenerator.core.central;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.projects.Settings;
import io.ballerina.projects.internal.model.Central;
import io.ballerina.projects.internal.model.Proxy;
import org.ballerinalang.central.client.CentralAPIClient;
import org.ballerinalang.central.client.exceptions.CentralClientException;
import org.ballerinalang.central.client.model.Package;
import org.ballerinalang.diagramutil.connector.models.connector.Connector;
import org.ballerinalang.toml.exceptions.SettingsTomlException;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.ballerina.projects.util.ProjectUtils.getAccessTokenOfCLI;
import static io.ballerina.projects.util.ProjectUtils.initializeProxy;

/**
 * Implementation of the CentralAPI to obtain information from the central remotely.
 *
 * @since 1.4.0
 */
public class CentralImpl implements CentralAPI {

    private final CentralAPIClient client;
    private final GraphQlClient graphQlClient;
    private static CentralImpl instance;

    public static final String CLIENT_SYMBOL = "Client";

    public CentralImpl() {
        Settings settings;
        try {
            settings = RepoUtils.readSettings();
        } catch (SettingsTomlException e) {
            throw new RuntimeException(e);
        }
        Central central = settings.getCentral();
        Proxy proxy = settings.getProxy();
        client = new CentralAPIClient(RepoUtils.getRemoteRepoURL(), initializeProxy(proxy), proxy.username(),
                proxy.password(), getAccessTokenOfCLI(settings), central.getConnectTimeout(), central.getReadTimeout(),
                central.getWriteTimeout(), central.getCallTimeout(), central.getMaxRetries());

        try {
            graphQlClient = new GraphQlClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized CentralImpl getInstance() {
        if (instance == null) {
            instance = new CentralImpl();
        }
        return instance;
    }

    public FlowNode getNodeTemplate(Codedata codedata) {
        return null;
    }

    public List<Item> getConnectors() {
        return List.of();
    }

    @Override
    public List<Item> getConnectorActions(Codedata codedata) {
        return List.of();
    }

    public List<Item> getFunctions(Codedata codedata) {
        return List.of();
    }

    @Override
    public List<AvailableNode> getFunctions(Map<String, String> queryMap) {
        APIDocsModel.ApiResponse apiResponse = graphQlClient.getFunctions("ballerina", "log", "2.10.0");
        return apiResponse.data().apiDocs().docsData().modules().stream()
                .flatMap(module -> module.functions().stream())
                .map(function -> {
                    Metadata metadata = new Metadata.Builder<>(null)
                            .label(function.name())
                            .description(function.description())
                            .build();
                    Codedata codedata = new Codedata.Builder<>(null)
                            .node(FlowNode.Kind.FUNCTION_CALL)
                            .org(queryMap.get("org"))
                            .module(queryMap.get("module"))
                            .object(function.name())
                            .build();
                    return new AvailableNode(metadata, codedata, true);
                }).toList();
    }

    public List<AvailableNode> getConnectors(Map<String, String> queryMap) {
        JsonElement connectorSearchResult;
        try {
            connectorSearchResult = client.getConnectors(queryMap, "any", RepoUtils.getBallerinaVersion());
        } catch (CentralClientException e) {
            return List.of();
        }
        ConnectorList connectorList = new Gson().fromJson(connectorSearchResult.getAsString(), ConnectorList.class);
        return connectorList.connectors.stream()
                .filter(connector -> connector.name.equals(CLIENT_SYMBOL))
                .map(connector -> {
                    Package packageInfo = connector.packageInfo;
                    Metadata metadata = new Metadata.Builder<>(null)
                            .label(connector.moduleName)
                            .description(packageInfo.getSummary())
                            .keywords(packageInfo.getKeywords())
                            .icon(connector.icon).build();
                    Codedata codedata = new Codedata.Builder<>(null)
                            .node(FlowNode.Kind.NEW_CONNECTION)
                            .org(packageInfo.getOrganization())
                            .module(packageInfo.getName())
                            .object(connector.name)
                            .build();
                    return new AvailableNode(metadata, codedata, true);
                }).toList();
    }

    @Override
    public List<Item> getFunctions() {
        return List.of();
    }

    private record ConnectorList(List<Connector> connectors, int count, int offset, int limit) {
    }

    private String readJsonResource(String resourcePath) {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        try {
            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
