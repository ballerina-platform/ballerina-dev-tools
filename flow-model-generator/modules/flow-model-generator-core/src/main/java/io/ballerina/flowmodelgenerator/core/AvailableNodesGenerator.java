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
import io.ballerina.flowmodelgenerator.core.model.Category;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;

/**
 * Generates available nodes for a given position in the diagram.
 *
 * @since 1.4.0
 */
public class AvailableNodesGenerator {

    private final Category.Builder rootBuilder;

    public AvailableNodesGenerator() {
        this.rootBuilder = new Category.Builder(Category.Name.ROOT, null);
        initializeCommonNodes();
    }

    public JsonArray getAvailableNodes() {
        Gson gson = new Gson();
        Category rootCategory = this.rootBuilder.build();
        return gson.toJsonTree(rootCategory.items()).getAsJsonArray();
    }

    private void initializeCommonNodes() {
        // Initialize the builder with the common nodes
        this.rootBuilder
                .stepIn(Category.Name.FLOW)
                    .stepIn(Category.Name.BRANCH)
                        .node(FlowNode.Kind.IF)
                        .stepOut()
                    .stepIn(Category.Name.ITERATION)
                        .node(FlowNode.Kind.WHILE)
                        .node(FlowNode.Kind.BREAK)
                        .node(FlowNode.Kind.CONTINUE)
                        .stepOut()
                    .stepIn(Category.Name.CONTROL)
                        .node(FlowNode.Kind.RETURN)
                        .stepOut()
                    .stepOut()
                .stepIn(Category.Name.DATA)
                    .stepOut()
                .stepIn(Category.Name.ACTION)
                    .stepIn(Category.Name.HTTP_API)
                        .node(FlowNode.Kind.ACTION_CALL, "http", "get")
                        .node(FlowNode.Kind.ACTION_CALL, "http", "post")
                        .stepOut()
                    .stepIn(Category.Name.REDIS_CLIENT)
                        .node(FlowNode.Kind.ACTION_CALL, "redis", "get")
                        .node(FlowNode.Kind.ACTION_CALL, "redis", "set")
                    .stepOut();
    }
}
