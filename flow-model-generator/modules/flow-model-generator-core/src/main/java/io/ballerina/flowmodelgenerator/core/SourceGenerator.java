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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

/**
 * Generates source code from the flow model.
 *
 * @since 2201.9.0
 */
public class SourceGenerator {

    private final Gson gson;

    public SourceGenerator() {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(FlowNode.class, new FlowNode.Deserializer())
                .create();
    }

    public String toSourceCode(JsonElement diagramNode) {
        FlowNode flowNode = gson.fromJson(diagramNode, FlowNode.class);
        return flowNode.toSource(new FlowNode.SourceBuilder.SourceBuilderData());
    }
}
