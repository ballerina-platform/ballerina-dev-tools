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

package io.ballerina.modelgenerator.commons;

import io.ballerina.compiler.api.symbols.ParameterKind;

/**
 * Represents the result of a parameter.
 *
 * @param parameterId      the ID of the parameter
 * @param name             the name of the parameter
 * @param type             the type of the parameter
 * @param kind             the kind of the parameter
 * @param defaultValue     the default value of the parameter
 * @param description      the description of the parameter
 * @param optional         whether the parameter is optional
 * @param importStatements import statements of the dependent types
 * @since 2.0.0
 */
public record ParameterData(
        int parameterId,
        String name,
        String type,
        Kind kind,
        String defaultValue,
        String description,
        boolean optional,
        String importStatements) {

    public static ParameterData from(String name, String type, Kind kind, String defaultValue,
                                     String description, boolean optional) {
        return new ParameterData(0, name, type, kind, defaultValue, description, optional,
                null);
    }

    public static ParameterData from(String name, String description, String type, String defaultValue, Kind kind,
                                     boolean optional, String importStatements) {
        return new ParameterData(0, name, type, kind, defaultValue, description, optional,
                importStatements);
    }

    public enum Kind {
        REQUIRED,
        DEFAULTABLE,
        INCLUDED_RECORD,
        REST_PARAMETER,
        INCLUDED_FIELD,
        PARAM_FOR_TYPE_INFER,
        INCLUDED_RECORD_REST,
        PATH_PARAM,
        PATH_REST_PARAM;

        public static Kind fromKind(ParameterKind parameterKind) {
            String value = parameterKind.name();
            if (value.equals("REST")) {
                return REST_PARAMETER;
            }
            return Kind.valueOf(value);
        }
    }

}
