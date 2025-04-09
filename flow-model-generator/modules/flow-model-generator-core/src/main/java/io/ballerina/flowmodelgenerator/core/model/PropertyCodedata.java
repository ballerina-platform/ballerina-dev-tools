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

import io.ballerina.tools.text.LineRange;

/**
 * Represents the codedata of a property.
 *
 * @param kind              The kind of the property
 * @param originalName      The original name of the property
 * @param dependentProperty The property that is dependent for this property to be enabled
 * @param lineRange         The line range of the property
 * @since 2.0.0
 */
public record PropertyCodedata(String kind, String originalName, String dependentProperty, LineRange lineRange) {

    public static class Builder<T> extends FacetedBuilder<T> {

        private String kind;
        private String originalName;
        private String dependentProperty;
        private LineRange lineRange;

        public Builder(T parentBuilder) {
            super(parentBuilder);
        }

        public Builder<T> kind(String kind) {
            this.kind = kind;
            return this;
        }

        public Builder<T> originalName(String originalName) {
            this.originalName = originalName;
            return this;
        }

        public Builder<T> dependentProperty(String dependentProperty) {
            this.dependentProperty = dependentProperty;
            return this;
        }

        public Builder<T> lineRange(LineRange lineRange) {
            this.lineRange = lineRange;
            return this;
        }

        public PropertyCodedata build() {
            return new PropertyCodedata(kind, originalName, dependentProperty, lineRange);
        }
    }
}
