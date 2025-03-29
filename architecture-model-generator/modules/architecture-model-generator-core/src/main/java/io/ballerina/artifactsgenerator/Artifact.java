/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.artifactsgenerator;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.designmodelgenerator.core.CommonUtils;
import io.ballerina.runtime.api.utils.IdentifierUtils;
import io.ballerina.tools.text.LineRange;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an artifact in the project tree.
 *
 * @param id       unique identifier for the artifact
 * @param location location information of the artifact
 * @param type     type of the artifact
 * @param name     symbol name of the artifact
 * @param accessor accessor of the artifact
 * @param scope    scope of the artifact (global/local)
 * @param icon     icon representing the artifact
 * @param children map of child artifacts (id -> child)
 * @since 2.3.0
 */
public record Artifact(String id, LineRange location, Type type, String name, String accessor,
                       String scope, String icon, Map<String, Artifact> children) {

    public Artifact {
        children = children == null ? Collections.emptyMap() : Collections.unmodifiableMap(children);
    }

    /**
     * Get a child artifact by its ID.
     *
     * @param id ID of the child artifact
     * @return Optional containing the child artifact if found
     */
    public Optional<Artifact> getChild(String id) {
        return Optional.ofNullable(children.get(id));
    }

    /**
     * Represents the different types of artifacts.
     */
    public enum Type {
        SERVICE("Entry Points"),
        AUTOMATION("Entry Points"),
        RESOURCE("Resources"),
        REMOTE("Remote Methods"),
        FUNCTION("Functions"),
        NP_FUNCTION("Natural Functions"),
        DATA_MAPPER("Data Mappers"),
        LISTENER("Listeners"),
        CONFIGURABLE("Configurations"),
        TYPE("Types"),
        CONNECTION("Connections");

        private final String category;

        Type(String category) {
            this.category = category;
        }

        public String getCategory() {
            return category;
        }
    }

    public enum Scope {
        GLOBAL("Global"),
        LOCAL("Local"),
        OBJECT("Object");

        private final String value;

        Scope(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Builder class for creating Artifact instances.
     */
    public static class Builder {

        private String id;
        private LineRange location;
        private Type type;
        private String name;
        private String accessor;
        private Scope scope = Scope.GLOBAL;
        private String icon;
        private final Map<String, Artifact> children = new HashMap<>();

        public Builder(Node node) {
            this.location = node.lineRange();
        }

        public Builder node(Node node) {
            this.location = node.lineRange();
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder accessor(String accessor) {
            this.accessor = accessor;
            return this;
        }

        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }

        public Builder icon(Symbol symbol) {
            symbol.getModule().ifPresent(module -> this.icon = CommonUtils.generateIcon(module.id()));
            return this;
        }

        public Builder child(Artifact child) {
            if (child != null && child.id() != null) {
                this.children.put(child.id(), child);
            }
            return this;
        }

        public Artifact build() {
            if (accessor != null) {
                id = accessor + "#" + name;
                accessor = IdentifierUtils.unescapeBallerina(accessor);
            } else {
                id = name;
            }
            name = IdentifierUtils.unescapeBallerina(name);
            return new Artifact(id, location, type, name, accessor, scope.getValue(), icon, new HashMap<>(children));
        }
    }
}
