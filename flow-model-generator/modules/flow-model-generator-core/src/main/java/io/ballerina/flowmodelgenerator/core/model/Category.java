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

package io.ballerina.flowmodelgenerator.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a category of the available nodes.
 *
 * @param name        name of the category
 * @param description description of the category
 * @param keywords    keywords of the category
 * @param items       items in the category
 * @since 1.4.0
 */
public record Category(String name, String description, List<String> keywords, List<Item> items) implements Item {

    /**
     * Represents the name of a category which contains the metadata of the category.
     *
     * @since 1.4.0
     */
    public enum Name {
        ROOT("Root", "The topmost category of the palette", null),
        FLOW("Flow", "Flow control nodes", List.of("Core", "Control", "Flow")),
        BRANCH("Branch", "Branching nodes", null),
        ITERATION("Iteration", "Iteration nodes", null),
        CONTROL("Control", "Control nodes", null),
        CONCURRENCY("Concurrency", "Concurrency nodes", null),
        DATA("Data", "Data nodes are used to create, read, update, delete, and transform data", null),
        ACTION("Action", "Connect to different services, APIs, SaaS products, etc.", null),
        HTTP_API("HTTP API", "Make HTTP requests", null),
        REDIS_CLIENT("Redis Client", "Interact with a Redis server", null);

        final String name;
        final String description;
        final List<String> keywords;

        Name(String name, String description, List<String> keywords) {
            this.name = name;
            this.description = description;
            this.keywords = keywords;
        }
    }

    /**
     * Represents a builder for the category. The builder can build the categories in a nested manner.
     *
     * @since 1.4.0
     */
    public static class Builder {

        private final Name name;
        private final Builder parentBuilder;
        private final Map<Name, Builder> childBuilders;
        private final List<AvailableNode> availableNodes;

        public Builder(Name name, Builder parentBuilder) {
            this.name = name;
            this.parentBuilder = parentBuilder;
            this.childBuilders = new LinkedHashMap<>();
            this.availableNodes = new ArrayList<>();
        }

        public Builder stepIn(Name childName) {
            Builder builder = this.childBuilders.get(childName);
            if (builder == null) {
                builder = new Builder(childName, this);
                this.childBuilders.put(childName, builder);
            }
            return builder;
        }

        public Builder stepOut() {
            if (parentBuilder == null) {
                throw new IllegalStateException("Cannot step out of the root category");
            }
            return parentBuilder;
        }

        public Builder node(FlowNode.Kind kind) {
            FlowNode flowNode = FlowNode.getNodeFromKind(kind);
            this.availableNodes.add(flowNode.extractAvailableNode());
            return this;
        }

        public Builder node(FlowNode.Kind kind, String library, String call) {
            FlowNode flowNode = FlowNode.getNodeFromKind(kind);
            NodeAttributes.Info info = NodeAttributes.get(library + "-" + call);
            flowNode.label = info.label();
            this.availableNodes.add(flowNode.extractAvailableNode(library, call));
            return this;
        }

        public Category build() {
            // Check for illegal state where both nodes and categories are present
            if (!this.availableNodes.isEmpty() && !this.childBuilders.isEmpty()) {
                throw new IllegalStateException("A category cannot have both categories and nodes as items");
            }

            List<Item> items = new ArrayList<>();

            // If nodes are present, build them into items
            if (!this.availableNodes.isEmpty()) {
                items.addAll(this.availableNodes);
            } else {
                // If categories are present, build each category and add as items
                this.childBuilders.forEach((key, value) -> items.add(value.build()));
            }

            // Create and return the new category with the built items
            return new Category(name.name, name.description, name.keywords, items);
        }
    }
}
