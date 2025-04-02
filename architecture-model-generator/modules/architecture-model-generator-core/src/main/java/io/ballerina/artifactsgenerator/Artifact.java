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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.designmodelgenerator.core.CommonUtils;
import io.ballerina.runtime.api.utils.IdentifierUtils;
import io.ballerina.tools.text.LineRange;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
 * @param module   module name of the artifact
 * @since 2.3.0
 */
public record Artifact(String id, LineRange location, String type, String name, String accessor,
                       String scope, String icon, String module, Map<String, Artifact> children) {

    private static final Map<String, String> typeCategoryMap = Map.ofEntries(
            Map.entry(Type.SERVICE.name(), "Entry Points"),
            Map.entry(Type.AUTOMATION.name(), "Entry Points"),
            Map.entry(Type.RESOURCE.name(), "Resources"),
            Map.entry(Type.REMOTE.name(), "Remote Methods"),
            Map.entry(Type.FUNCTION.name(), "Functions"),
            Map.entry(Type.NP_FUNCTION.name(), "Natural Functions"),
            Map.entry(Type.DATA_MAPPER.name(), "Data Mappers"),
            Map.entry(Type.LISTENER.name(), "Listeners"),
            Map.entry(Type.CONFIGURABLE.name(), "Configurations"),
            Map.entry(Type.TYPE.name(), "Types"),
            Map.entry(Type.CONNECTION.name(), "Connections"),
            Map.entry(Type.VARIABLE.name(), "Variables"));

    public static String getCategory(String type) {
        return typeCategoryMap.getOrDefault(type, "Others");
    }

    public static Artifact emptyArtifact(String id) {
        return new Artifact(id, null, null, null, null, null, null, null, null);
    }

    public Artifact {
        children = children == null ? Collections.emptyMap() : Collections.unmodifiableMap(children);
    }

    /**
     * Represents the different types of artifacts.
     */
    public enum Type {
        SERVICE,
        AUTOMATION,
        RESOURCE,
        REMOTE,
        FUNCTION,
        NP_FUNCTION,
        DATA_MAPPER,
        LISTENER,
        CONFIGURABLE,
        TYPE,
        CONNECTION,
        VARIABLE;
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
        private String module;
        private final Map<String, Artifact> children = new HashMap<>();

        public Builder(Node node) {
            this.location = node.lineRange();
        }

        public Builder node(Node node) {
            this.location = node.lineRange();
            return this;
        }

        public Builder locationId() {
            if (location == null) {
                return this;
            }
            this.id = String.valueOf(Objects.hash(location.fileName(), location.startLine(), location.endLine()));
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
            Optional<ModuleSymbol> moduleSymbol = symbol.getModule();
            if (moduleSymbol.isEmpty()) {
                return this;
            }
            ModuleID moduleId = moduleSymbol.get().id();
            this.icon = CommonUtils.generateIcon(moduleId);
            this.module = moduleId.moduleName();
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
                id = id == null ? accessor + "#" + name : id;
                accessor = IdentifierUtils.unescapeBallerina(accessor);
            } else {
                id = id == null ? name : id;
            }
            name = IdentifierUtils.unescapeBallerina(name);
            return new Artifact(id, location, type == null ? null : type.name(), name, accessor, scope.getValue(), icon,
                    module, new HashMap<>(children));
        }
    }
}
