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
 * @param metadata            metadata of the property
 * @param valueType           acceptable value types of the property
 * @param valueTypeConstraint constraint of the value type
 * @param value               value of the property
 * @param optional            whether the property is optional
 * @param editable            whether the property is editable
 * @since 1.4.0
 */
public record Property(Metadata metadata, String valueType, Object valueTypeConstraint, Object value, boolean optional,
                       boolean editable) {

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

    public static final String DATA_VARIABLE_LABEL = "Data variable";
    public static final String DATA_VARIABLE_KEY = "variable";
    public static final String DATA_VARIABLE_DOC = "Name of the variable";

    public static final String DATA_TYPE_LABEL = "Data type";
    public static final String DATA_TYPE_KEY = "type";
    public static final String DATA_TYPE_DOC = "Type of the variable";

    public static final String SCOPE_LABEL = "Connection Scope";
    public static final String SCOPE_KEY = "scope";
    public static final String SCOPE_DOC = "Scope of the connection, Global or Local";
    public static final String GLOBAL_SCOPE = "Global";
    public static final String SERVICE_SCOPE = "Service";
    public static final String LOCAL_SCOPE = "Local";

    public static final String CONNECTION_KEY = "connection";

    public static final String COMMENT_LABEL = "Comment";
    public static final String COMMENT_KEY = "comment";
    public static final String COMMENT_DOC = "Comment to describe the flow";

    public String toSourceCode() {
        return value.toString();
    }

    public enum ValueType {
        EXPRESSION,
        IDENTIFIER,
        TYPE,
        ENUM,
        SET
    }

    /**
     * Represents a builder for the expression.
     *
     * @since 1.4.0
     */
    public static class Builder {

        private String type;
        private Object value;
        private boolean optional;
        private boolean editable;
        private Object typeConstraint;
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
            this.type = CommonUtils.getTypeSignature(null, typeSymbol, false);
            return this;
        }

        public Builder type(ValueType type) {
            this.type = type.name();
            return this;
        }

        public Builder typeConstraint(Object typeConstraint) {
            this.typeConstraint = typeConstraint;
            return this;
        }

        public Builder value(Object value) {
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
                    new Property(metadataBuilder == null ? null : metadataBuilder.build(), type, typeConstraint, value,
                            optional, editable);
            this.metadataBuilder = null;
            this.type = null;
            this.typeConstraint = null;
            this.value = null;
            this.optional = false;
            this.editable = false;
            return property;
        }
    }
}
