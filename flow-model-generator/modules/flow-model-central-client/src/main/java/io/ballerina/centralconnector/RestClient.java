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

package io.ballerina.centralconnector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.centralconnector.response.ConnectorResponse;
import io.ballerina.centralconnector.response.ConnectorsResponse;
import io.ballerina.centralconnector.response.PackageResponse;
import io.ballerina.centralconnector.response.SymbolResponse;
import io.ballerina.projects.Settings;
import io.ballerina.projects.internal.model.Central;
import io.ballerina.projects.internal.model.Proxy;
import org.ballerinalang.central.client.CentralAPIClient;
import org.ballerinalang.central.client.exceptions.CentralClientException;
import org.ballerinalang.toml.exceptions.SettingsTomlException;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static io.ballerina.projects.util.ProjectUtils.getAccessTokenOfCLI;
import static io.ballerina.projects.util.ProjectUtils.initializeProxy;

/**
 * This class provides methods to interact with the Ballerina Central REST API.
 *
 * @since 2.0.0
 */
class RestClient {

    private static final String BASE_URL = "https://api.central.ballerina.io/2.0/registry/";
    private static final String SEARCH_SYMBOLS = "search-symbols";
    private static final String SEARCH_PACKAGES = "search-packages";
    private static final String CONNECTOR = "connector";
    private final Gson gson;
    private final CentralAPIClient centralClient;

    public RestClient() {
        gson = new Gson();

        Settings settings;
        try {
            settings = RepoUtils.readSettings();
        } catch (SettingsTomlException e) {
            throw new RuntimeException(e);
        }
        Central central = settings.getCentral();
        Proxy proxy = settings.getProxy();
        centralClient = new CentralAPIClient(RepoUtils.getRemoteRepoURL(), initializeProxy(proxy), proxy.username(),
                proxy.password(), getAccessTokenOfCLI(settings), central.getConnectTimeout(), central.getReadTimeout(),
                central.getWriteTimeout(), central.getCallTimeout(), central.getMaxRetries());
    }

    public ConnectorsResponse connectors(Map<String, String> queryMap) {
        JsonElement connectorSearchResult;
        try {
            connectorSearchResult = centralClient.getConnectors(queryMap, "any", RepoUtils.getBallerinaVersion());
        } catch (CentralClientException e) {
            throw new RuntimeException(e);
        }
        return gson.fromJson(connectorSearchResult.getAsString(), ConnectorsResponse.class);
    }

    public ConnectorResponse connector(String id) {
        JsonObject connectorSearchResult;
        try {
            connectorSearchResult = centralClient.getConnector(id, "any", RepoUtils.getBallerinaVersion());
        } catch (CentralClientException e) {
            throw new RuntimeException(e);
        }
        return gson.fromJson(connectorSearchResult, ConnectorResponse.class);
    }

    public ConnectorResponse connector(String org, String module, String version, String connector) {
        String path = String.format("%s/connectors/%s/%s/%s/%s/%s", BASE_URL, org, module, version, module, connector);
        String response = query(path);
        return gson.fromJson(response, ConnectorResponse.class);
    }

    public PackageResponse searchPackages(Map<String, String> queryMap) {
        String queryMapString = getQueryMapString(queryMap);
        String response = query(SEARCH_PACKAGES, queryMapString);
        return gson.fromJson(response, PackageResponse.class);
    }

    public SymbolResponse searchSymbols(Map<String, String> queryMap) {
        String queryMapString = getQueryMapString(queryMap);
        String response = query(SEARCH_SYMBOLS, queryMapString);
        return gson.fromJson(response, SymbolResponse.class);
    }

    private String getQueryMapString(Map<String, String> queryMap) {
        StringBuilder queryParams = new StringBuilder();
        for (Map.Entry<String, String> entry : queryMap.entrySet()) {
            if (!queryParams.isEmpty()) {
                queryParams.append("&");
            }
            queryParams.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return queryParams.toString();
    }

    private String query(String api) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(api);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return response.toString();
                }
            }
            throw new RuntimeException("GET request not worked");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

    }

    private String query(String api, String queryMap) {
        String fullUrl = String.format("%s/%s?%s", BASE_URL, api, queryMap);
        return query(fullUrl);
    }
}
