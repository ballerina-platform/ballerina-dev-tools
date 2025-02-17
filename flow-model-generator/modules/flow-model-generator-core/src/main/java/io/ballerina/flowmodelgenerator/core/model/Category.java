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
 * @param metadata metadata of the category
 * @param items    items in the category
 * @since 2.0.0
 */
public record Category(Metadata metadata, List<Item> items) implements Item {

    /**
     * Represents the name of a category which contains the metadata of the category.
     *
     * @since 2.0.0
     */
    public enum Name {
        ROOT("Root", "The topmost category of the palette", null),
        FLOW("Flow", "Flow control nodes", List.of("Core", "Control", "Flow")),
        STATEMENT("Statement", "Fundamental executable units in a program", null),
        CONNECTIONS("Connections", "The connections used in the flow", null),
        AGENTS("Agents", "The agents used in the flow", null),
        BRANCH("Branch", "Branching nodes", null),
        FLOWS("Flows", "Flows that invoke local or utility functions",
                List.of("Function", "Call", "Utility", "Local")),
        TERMINATION("Termination", "Termination nodes", null),
        ITERATION("Iteration", "Iteration nodes", null),
        CONTROL("Control", "Control nodes", null),
        CONCURRENCY("Concurrency", "Concurrency nodes", null),
        ERROR_HANDLING("Error Handling", "Handle errors that occur during execution", null),
        DATA("Data", "Data nodes are used to create, read, update, delete, and transform data", null),
        CURRENT_INTEGRATION("Current Integration", "Functions defined within the current integration",
                List.of("Project", "Local", "Function")),
        IMPORTED_FUNCTIONS("Imported Functions", "Functions imported from other integrations",
                List.of("Imported", "Function", "Library")),
        AVAILABLE_FUNCTIONS("Available Functions", "Functions available in the library",
                List.of("Available", "Function", "Library")),
        ;

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
     * @since 2.0.0
     */
    public static class Builder {

        private final Builder parentBuilder;
        private final Map<String, Builder> childBuilders;
        private final List<AvailableNode> availableNodes;
        private List<Item> prebuiltItems;
        private Metadata.Builder<Category.Builder> metadataBuilder;

        public Builder(Builder parentBuilder) {
            this.parentBuilder = parentBuilder;
            this.childBuilders = new LinkedHashMap<>();
            this.availableNodes = new ArrayList<>();
        }

        public Builder stepIn(Name childName) {
            Builder builder = this.childBuilders.get(childName.name);
            if (builder == null) {
                builder = new Builder(this).metadata()
                        .label(childName.name)
                        .description(childName.description)
                        .keywords(childName.keywords)
                        .stepOut();
                this.childBuilders.put(childName.name, builder);
            }
            return builder;
        }

        public Builder stepIn(String name, String description, String icon) {
            Builder builder = this.childBuilders.get(name);
            if (builder == null) {
                builder = new Builder(this).metadata()
                        .label(name)
                        .description(description)
                        .icon(icon)
                        .stepOut();
                this.childBuilders.put(name, builder);
            }
            return builder;
        }

        public Builder stepIn(String childName) {
            Builder builder = this.childBuilders.get(childName);
            if (builder == null) {
                builder = new Builder(this).metadata().label(childName).stepOut();
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

        public Builder node(NodeKind kind) {
            AvailableNode node = NodeBuilder.getNodeFromKind(kind).buildAvailableNode();
            this.availableNodes.add(node);
            return this;
        }

        public Builder node(AvailableNode node) {
            this.availableNodes.add(node);
            return this;
        }

        public Builder items(List<Item> items) {
            this.prebuiltItems = items;
            return this;
        }

        public Metadata.Builder<Category.Builder> metadata() {
            if (this.metadataBuilder == null) {
                this.metadataBuilder = new Metadata.Builder<>(this);
            }
            return this.metadataBuilder;
        }

        public Builder name(Name name) {
            this.metadataBuilder = new Metadata.Builder<>(this)
                    .label(name.name)
                    .description(name.description)
                    .keywords(name.keywords);
            return this;
        }

        public Category build() {
            Metadata newMetadata = this.metadataBuilder != null ? this.metadataBuilder.build() : null;

            // Check if there exist prebuilt items
            if (this.prebuiltItems != null) {
                return new Category(newMetadata, this.prebuiltItems);
            }

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
            return new Category(newMetadata, items);
        }
    }
}
