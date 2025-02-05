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

/**
 * Represents a function parameter.
 *
 * @since 2.0.0
 */
public record FunctionParameter(Property type, Property variable, Property defaultValue,
                                boolean optional, boolean editable, boolean advanced) {

    public static class FunctionParameterBuilder {
        private Property type;
        private Property variable;
        private Property defaultValue;
        private boolean optional = false;
        private boolean editable = true;
        private boolean advanced = false;

        public FunctionParameterBuilder type(String type) {
            this.type = new Property.PropertyBuilder()
                    .valueType("TYPE")
                    .editable(true)
                    .value(type)
                    .build();
            return this;
        }

        public FunctionParameterBuilder variable(String variable) {
            this.variable = new Property.PropertyBuilder()
                    .valueType("IDENTIFIER")
                    .editable(true)
                    .value(variable)
                    .build();
            return this;
        }

        public FunctionParameterBuilder defaultValue(Object defaultValue) {
            this.defaultValue = new Property.PropertyBuilder()
                    .valueType("EXPRESSION")
                    .editable(true)
                    .value(defaultValue)
                    .build();
            return this;
        }

        public FunctionParameterBuilder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public FunctionParameterBuilder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public FunctionParameterBuilder advanced(boolean advanced) {
            this.advanced = advanced;
            return this;
        }

        public FunctionParameter build() {
            return new FunctionParameter(type, variable, defaultValue, optional, editable, advanced);
        }
    }
}
