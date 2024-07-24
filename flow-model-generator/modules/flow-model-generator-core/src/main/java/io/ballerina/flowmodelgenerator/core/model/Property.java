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

import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.flowmodelgenerator.core.CommonUtils;

/**
 * Represents an expression in the flow model.
 *
 * @param metadata  metadata of the property
 * @param valueType acceptable value types of the property
 * @param value     value of the property
 * @param optional  whether the property is optional
 * @param editable  whether the property is editable
 * @since 1.4.0
 */
public record Property(Metadata metadata, String valueType, String value, boolean optional, boolean editable) {

    public String toSourceCode() {
        return value;
    }

    public static final String CONDITION_LABEL = "Condition";
    public static final String CONDITION_KEY = "condition";

    public static final String EXPRESSION_LABEL = "Expression";
    public static final String EXPRESSION_KEY = "expression";

    /**
     * Represents a builder for the expression.
     *
     * @since 1.4.0
     */
    public static class Builder {

        private String label;
        private String type;
        private String value;
        private boolean optional;
        private boolean editable;
        private String documentation;

        private Builder() {

        }

        private static final class InstanceHolder {

            private static final Builder instance = new Builder();
        }

        public static Builder getInstance() {
            return InstanceHolder.instance;
        }

        public Builder label(String key) {
            this.label = key;
            return this;
        }

        public Builder type(TypeSymbol typeSymbol) {
            this.type = CommonUtils.getTypeSignature(typeSymbol);
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder editable() {
            this.editable = true;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Property build() {
            Property property = new Property(new Metadata(label, documentation, null), type, value, optional, editable);
            this.label = null;
            this.type = null;
            this.value = null;
            this.optional = false;
            this.editable = false;
            return property;
        }
    }
}
