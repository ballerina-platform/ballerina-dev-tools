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
 * The central interface to obtain library information from the Ballerina central.
 *
 * @since 2.0.0
 */
public interface CentralAPI {

    PackageResponse searchPackages(Map<String, String> queryMap);

    SymbolResponse searchSymbols(Map<String, String> queryMap);

    FunctionsResponse functions(String organization, String name, String version);

    FunctionResponse function(String organization, String name, String version, String functionName);

    ConnectorsResponse connectors(Map<String, String> queryMap);

    ConnectorResponse connector(String id);

    ConnectorResponse connector(String organization, String name, String version, String clientName);

    String latestPackageVersion(String org, String name);
}
