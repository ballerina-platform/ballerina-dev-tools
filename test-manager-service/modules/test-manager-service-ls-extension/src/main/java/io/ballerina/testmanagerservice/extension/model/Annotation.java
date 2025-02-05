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

package io.ballerina.testmanagerservice.extension.model;

import io.ballerina.testmanagerservice.extension.Constants;

import java.util.List;

public record Annotation(Metadata metadata, Codedata codedata, String org, String module, String name,
                         List<Property> fields) {

    public static class ConfigAnnotationBuilder {
        private Metadata metadata;
        private Codedata codedata;
        private final String org = Constants.ORG_BALLERINA;
        private final String module = Constants.MODULE_TEST;
        private final String name = "Config";
        private Property groups;
        private Property enabled;

        public ConfigAnnotationBuilder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public ConfigAnnotationBuilder groups(List<String> groupList) {
            groups = value("Groups", "Groups to run", groupList,
                    "EXPRESSION_SET", "groups");
            return this;
        }

        public ConfigAnnotationBuilder enabled(boolean enabled) {
            this.enabled = value("Enabled", "Enable/Disable the test", enabled,
                    "EXPRESSION", "enabled");
            return this;
        }

        private static Property value(String label, String description, Object value, String valueType,
                                     String originalName) {
            Property.PropertyBuilder builder = new Property.PropertyBuilder();
            builder.metadata(new Metadata(label, description));
            builder.valueType(valueType);
            builder.originalName(originalName);
            builder.value(value);
            builder.advanced(false);
            builder.editable(true);
            builder.optional(true);
            return builder.build();
        }

        public Annotation build() {
            if (groups == null) {
                groups = value("Groups", "Groups to run", List.of(),
                        "EXPRESSION_SET", "groups");
            }
            if (enabled == null) {
                enabled = value("Enabled", "Enable/Disable the test", true,
                        "EXPRESSION", "enabled");
            }
            return new Annotation(metadata, codedata, org, module, name, List.of(groups, enabled));
        }
    }
}
