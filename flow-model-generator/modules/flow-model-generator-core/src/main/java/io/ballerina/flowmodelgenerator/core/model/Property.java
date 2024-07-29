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

    public static final String VARIABLE_LABEL = "Variable";
    public static final String VARIABLE_KEY = "variable";
    public static final String VARIABLE_DOC = "Result Variable";

    public static final String EXPRESSION_LABEL = "Expression";
    public static final String EXPRESSION_KEY = "expression";
    public static final String EXPRESSION_DOC = "Expression";

    public static final String CONDITION_LABEL = "Condition";
    public static final String CONDITION_KEY = "condition";
    public static final String CONDITION_DOC = "Boolean Condition";

    public static final String IGNORE_LABEL = "Ignore";
    public static final String IGNORE_KEY = "ignore";
    public static final String IGNORE_DOC = "Ignore the error value";

    public static final String ON_ERROR_VARIABLE_LABEL = "Error Variable";
    public static final String ON_ERROR_VARIABLE_KEY = "errorVariable";
    public static final String ON_ERROR_VARIABLE_DOC = "Name of the error variable";

    public static final String ON_ERROR_TYPE_LABEL = "Error Type";
    public static final String ON_ERROR_TYPE_KEY = "errorType";
    public static final String ON_ERROR_TYPE_DOC = "Type of the error";

    public static final String COLLECTION_LABEL = "Collection";
    public static final String COLLECTION_KEY = "collection";
    public static final String COLLECTION_DOC = "Collection to iterate";

    public String toSourceCode() {
        return value;
    }

    public enum ValueType {
        EXPRESSION,
        IDENTIFIER
    }

    /**
     * Represents a builder for the expression.
     *
     * @since 1.4.0
     */
    public static class Builder {

        private String type;
        private String value;
        private boolean optional;
        private boolean editable;
        private Metadata.Builder<Builder> metadataBuilder;

        private Builder() {

        }

        private static final class InstanceHolder {

            private static final Builder instance = new Builder();
        }

        public static Builder getInstance() {
            return InstanceHolder.instance;
        }

        public Builder type(TypeSymbol typeSymbol) {
            this.type = CommonUtils.getTypeSignature(typeSymbol);
            return this;
        }

        public Builder type(ValueType type) {
            this.type = type.name();
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

        public Metadata.Builder<Builder> metadata() {
            if (this.metadataBuilder == null) {
                this.metadataBuilder = new Metadata.Builder<>(this);
            }
            return this.metadataBuilder;
        }

        public Property build() {
            Property property =
                    new Property(metadataBuilder == null ? null : metadataBuilder.build(), type, value, optional,
                            editable);
            this.metadataBuilder = null;
            this.type = null;
            this.value = null;
            this.optional = false;
            this.editable = false;
            return property;
        }
    }
}
