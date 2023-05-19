/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.extension.architecture;

import com.google.gson.JsonObject;
import io.ballerina.architecturemodelgenerator.extension.ModelResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Response format for architecture model request.
 *
 * @since 2201.2.2
 */
public class ArchitectureModelResponse extends ModelResponse {

    private Map<String, JsonObject> componentModels = new HashMap<>();

    public ArchitectureModelResponse() {
        super(new ArrayList<>());
    }

    public Map<String, JsonObject> getComponentModels() {
        return componentModels;
    }

    public void setComponentModels(Map<String, JsonObject> componentModels) {
        this.componentModels = componentModels;
    }

    public void addComponentModel(String key, JsonObject jsonObject) {
        componentModels.put(key, jsonObject);
    }
}
