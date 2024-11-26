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

package io.ballerina.flowmodelgenerator.extension.response;

import com.google.gson.JsonArray;

/**
 * Represents the response for getting connectors.
 *
 * @since 1.4.0
 */
public class FlowModelGetConnectorsResponse extends AbstractFlowModelResponse {

    private JsonArray categories;

    public void setCategories(JsonArray categories) {
        this.categories = categories;
    }

    public JsonArray categories() {
        return categories;
    }
}
