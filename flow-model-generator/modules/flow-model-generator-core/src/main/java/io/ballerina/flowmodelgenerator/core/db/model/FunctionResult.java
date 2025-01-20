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

package io.ballerina.flowmodelgenerator.core.db.model;

/**
 * Represents the result of a function.
 *
 * @param functionId           the ID of the function
 * @param name                 the name of the function
 * @param description          the description of the function
 * @param returnType           the return type of the function
 * @param packageName          the package name of the function
 * @param org                  the organization of the function
 * @param version              the version of the function
 * @param resourcePath         the resource path of the function
 * @param kind                 the kind of the function
 * @param returnError          the return error of the function, if any
 * @param inferredReturnType   whether function return type is inferred
 * @since 2.0.0
 */
public record FunctionResult(
        int functionId,
        String name,
        String description,
        String returnType,
        String packageName,
        String org,
        String version,
        String resourcePath,
        Function.Kind kind,
        boolean returnError,
        boolean inferredReturnType) {
}
