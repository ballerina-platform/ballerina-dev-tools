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

package io.ballerina.modelgenerator.commons;

import java.util.List;

/**
 * Represents a service type function.
 *
 * @param functionId The ID of the function
 * @param name The name of the function
 * @param description The description of the function
 * @param accessor The accessor of the function
 * @param kind The kind of the function
 * @param returnType The return type of the function
 * @param returnError Whether the return type has an error
 * @param returnTypeEditable Whether the return type is editable
 * @param importStatements The import statements of the function
 * @param enable Whether the function is enabled
 * @param parameters The parameters of the function
 *
 * @since 2.0.0
 */
public record ServiceTypeFunction(
        int functionId,
        String name,
        String description,
        String accessor,
        String kind,
        String returnType,
        int returnError,
        int returnTypeEditable,
        String importStatements,
        int enable,
        List<ServiceTypeFunctionParameter> parameters
) {

    public record ServiceTypeFunctionParameter(
            int parameterId,
            String name,
            String label,
            String description,
            String kind,
            String type, // Store JSON as String
            String defaultValue,
            String importStatements,
            int nameEditable,
            int typeEditable
    ) {
    }
}

