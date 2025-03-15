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

import io.ballerina.centralconnector.response.ConnectorResponse;
import io.ballerina.centralconnector.response.ConnectorsResponse;
import io.ballerina.centralconnector.response.FunctionResponse;
import io.ballerina.centralconnector.response.FunctionsResponse;
import io.ballerina.centralconnector.response.PackageResponse;
import io.ballerina.centralconnector.response.SymbolResponse;

import java.util.Map;

/**
 * An implementation {@code CentralAPI} to interact with the Ballerina central to obtain information about the Ballerina
 * libraries. This class provides a facade for interacting with REST and GraphQL clients.
 *
 * @since 2.0.0
 */
public class RemoteCentral implements CentralAPI {

    private final RestClient restClient;
    private final GraphQlClient graphQlClient;

    private static class Holder {

        private static final RemoteCentral INSTANCE = new RemoteCentral();
    }

    public static RemoteCentral getInstance() {
        return Holder.INSTANCE;
    }

    private RemoteCentral() {
        this.restClient = new RestClient();
        this.graphQlClient = new GraphQlClient();
    }

    @Override
    public PackageResponse searchPackages(Map<String, String> queryMap) {
        return restClient.searchPackages(queryMap);
    }

    @Override
    public SymbolResponse searchSymbols(Map<String, String> queryMap) {
        return restClient.searchSymbols(queryMap);
    }

    @Override
    public FunctionsResponse functions(String organization, String name, String version) {
        return graphQlClient.getFunctions(organization, name, version);
    }

    @Override
    public FunctionResponse function(String organization, String name, String version, String functionName) {
        return graphQlClient.getFunction(organization, name, version, functionName);
    }

    @Override
    public ConnectorsResponse connectors(Map<String, String> queryMap) {
        return restClient.connectors(queryMap);
    }

    @Override
    public ConnectorResponse connector(String id) {
        return restClient.connector(id);
    }

    @Override
    public ConnectorResponse connector(String organization, String name, String version, String clientName) {
        return restClient.connector(organization, name, version, clientName);
    }

    @Override
    public String latestPackageVersion(String org, String name) {
        return restClient.latestPackageVersion(org, name);
    }
}
