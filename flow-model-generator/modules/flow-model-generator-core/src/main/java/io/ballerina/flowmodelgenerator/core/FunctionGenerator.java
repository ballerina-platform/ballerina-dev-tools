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
import io.ballerina.flowmodelgenerator.core.central.CentralAPI;
import io.ballerina.flowmodelgenerator.core.central.CentralApiFactory;

import java.util.Map;

/**
 * Generates functions based on a given keyword.
 *
 * @since 1.4.0
 */
public class FunctionGenerator {

    private final Gson gson;
    private final CentralAPI central;

    public FunctionGenerator() {
        gson = new Gson();
        central = CentralApiFactory.getInstance();
    }

    public JsonArray getFunctions(String keyword) {
        Map<String, String> queryMap = Map.of("q", keyword);
        return gson.toJsonTree(central.getFunctions(queryMap)).getAsJsonArray();
    }
}
