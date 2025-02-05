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

public record Property(Metadata metadata, Codedata codedata, String valueType, Object valueTypeConstraint,
                       String originalName, Object value, String placeholder, boolean optional, boolean editable,
                       boolean advanced) {

    public static class PropertyBuilder {
        private Metadata metadata;
        private Codedata codedata;
        private String valueType;
        private Object valueTypeConstraint;
        private Object value;
        private String originalName;
        private String placeholder;
        private boolean optional = false;
        private boolean editable = false;
        private boolean advanced = false;

        public PropertyBuilder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public PropertyBuilder codedata(Codedata codedata) {
            this.codedata = codedata;
            return this;
        }

        public PropertyBuilder valueType(String valueType) {
            this.valueType = valueType;
            return this;
        }

        public PropertyBuilder valueTypeConstraint(Object valueTypeConstraint) {
            this.valueTypeConstraint = valueTypeConstraint;
            return this;
        }

        public PropertyBuilder originalName(String originalName) {
            this.originalName = originalName;
            return this;
        }

        public PropertyBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public PropertyBuilder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public PropertyBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public PropertyBuilder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public PropertyBuilder advanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public Property build() {
            return new Property(metadata, codedata, valueType, valueTypeConstraint, originalName, value, placeholder,
                    optional, editable, advanced);
        }
    }
}
