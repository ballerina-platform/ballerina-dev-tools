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
 * @param label    label of the expression
 * @param type     type of the expression
 * @param value    value of the expression
 * @param typeKind type kind of the expression
 * @param optional whether the expression is optional
 * @param editable whether the expression is editable
 * @since 2201.9.0
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

    /**
     * Represents a builder for the expression.
     *
     * @since 2201.9.0
     */
    public static class Builder {

        private String key;
        private String type;
        private String value;
        private ExpressionTypeKind typeKind;
        private boolean optional;
        private boolean editable;
        private String documentation;

        public void key(String key) {
            this.key = key;
        }

        public void type(TypeSymbol typeSymbol) {
            this.type = CommonUtils.getTypeSignature(typeSymbol);
        }

        public void type(String type) {
            this.type = type;
        }

        public void value(String value) {
            this.value = value;
        }

        public void typeKind(ExpressionTypeKind typeKind) {
            this.typeKind = typeKind;
        }

        public void optional(boolean optional) {
            this.optional = optional;
        }

        public void setEditable() {
            this.editable = true;
        }

        public void setDocumentation(String documentation) {
            this.documentation = documentation;
        }

        public Expression build() {
            Expression expression = new Expression(key, type, value, typeKind, optional, editable, documentation);
            this.key = null;
            this.type = null;
            this.value = null;
            this.typeKind = null;
            this.optional = false;
            this.editable = false;
            return expression;
        }
    }
}
