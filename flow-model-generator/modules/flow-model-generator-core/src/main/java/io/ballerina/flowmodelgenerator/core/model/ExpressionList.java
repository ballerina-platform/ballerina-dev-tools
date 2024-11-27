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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a list of expressions.
 *
 * @param label    label of the expression list
 * @param type     type of the expression list
 * @param value    the list of expressions
 * @param optional whether the expression list is optional
 * @since 2.0.0
 */
public record ExpressionList(String label, String type, List<Property> value, boolean optional) {

    /**
     * Represents a builder for the expression list.
     *
     * @since 2.0.0
     */
    public static class Builder {

        private String key;
        private String type;
        private List<Property> value;
        private boolean optional;

        public Builder() {
            this.value = new ArrayList<>();
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder value(Property value) {
            this.value.add(value);
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public ExpressionList build() {
            ExpressionList expressionList = new ExpressionList(key, type, value, optional);
            this.key = null;
            this.type = null;
            this.value = new ArrayList<>();
            this.optional = false;
            return expressionList;
        }

    }
}
