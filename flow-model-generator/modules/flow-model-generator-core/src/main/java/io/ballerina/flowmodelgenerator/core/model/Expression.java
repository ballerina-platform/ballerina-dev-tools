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
 * @param label         label of the expression
 * @param type          type of the expression
 * @param value         value of the expression
 * @param typeKind      type kind of the expression
 * @param optional      whether the expression is optional
 * @param editable      whether the expression is editable
 * @param documentation the documentation of the expression
 * @since 1.4.0
 */
public record Expression(String label, String type, String value, ExpressionTypeKind typeKind, boolean optional,
                         boolean editable, String documentation) {

    public String toSourceCode() {
        return value;
    }

    public enum ExpressionTypeKind {
        BTYPE,
        IDENTIFIER,
        URI_PATH
    }

    public record Info() {

    }

    /**
     * Represents a builder for the expression.
     *
     * @since 1.4.0
     */
    public static class Builder {

        private String label;
        private String type;
        private String value;
        private ExpressionTypeKind typeKind;
        private boolean optional;
        private boolean editable;
        private String documentation;

        private static Builder instance = null;

        public static Builder getInstance() {
            // TODO: Make this method concurrent safe
            if (instance == null) {
                instance = new Builder();
            }
            return instance;
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

        public Builder typeKind(ExpressionTypeKind typeKind) {
            this.typeKind = typeKind;
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

        public Expression build() {
            Expression expression = new Expression(label, type, value, typeKind, optional, editable, documentation);
            this.label = null;
            this.type = null;
            this.value = null;
            this.typeKind = null;
            this.optional = false;
            this.editable = false;
            return expression;
        }
    }
}
