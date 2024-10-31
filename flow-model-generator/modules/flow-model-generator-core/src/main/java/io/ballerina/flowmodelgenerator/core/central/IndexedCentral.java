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

import java.util.Map;

public class IndexedCentral implements CentralAPI{


    @Override
    public PackageResponse searchPackages(Map<String, String> queryMap) {
        return null;
    }

    @Override
    public SymbolResponse searchSymbols(Map<String, String> queryMap) {
        return null;
    }

    @Override
    public FunctionsResponse functions(String organization, String name, String version) {
        return null;
    }

    @Override
    public FunctionResponse function(String organization, String name, String version, String functionName) {
        return null;
    }

    @Override
    public ConnectorsResponse connectors(Map<String, String> queryMap) {
        return null;
    }

    @Override
    public ConnectorResponse connector(String id) {
        return null;
    }

    @Override
    public ConnectorResponse connector(String organization, String name, String version, String clientName) {
        return null;
    }
}
