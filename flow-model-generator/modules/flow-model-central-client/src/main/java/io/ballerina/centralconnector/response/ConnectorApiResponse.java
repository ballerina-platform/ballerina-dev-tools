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

package io.ballerina.centralconnector.response;

import java.util.List;

/**
 * Represents the Connector API response from the GraphQL endpoint.
 *
 * @param data the data in the response
 * @since 2.0.0
 */
public record ConnectorApiResponse(
        Data data
) {

    public record Data(ApiDocs apiDocs) { }

    public record ApiDocs(
            DocsData docsData
    ) { }

    public record DocsData(
            List<Module> modules
    ) { }

    public record Module(
            List<Method> remoteMethods,
            List<Method> resourceMethods,
            List<Field> fields,
            List<Method> methods,
            Method initMethod,
            List<Method> otherMethods,
            boolean isIsolated,
            boolean isService,
            String name,
            String description,
            List<String> descriptionSections,
            boolean isDeprecated,
            boolean isReadOnly
    ) { }

    public record Method(
            String accessor,
            String resourcePath,
            boolean isIsolated,
            boolean isRemote,
            boolean isResource,
            boolean isExtern,
            List<Parameter> parameters,
            List<ReturnParameter> returnParameters,
            List<String> annotationAttachments,
            String name,
            String description,
            List<String> descriptionSections,
            boolean isDeprecated,
            boolean isReadOnly
    ) { }

    public record Parameter(
            String defaultValue,
            Type type,
            String name,
            String description,
            boolean isDeprecated,
            boolean isReadOnly
    ) { }

    public record ReturnParameter(
            Type type,
            String name,
            String description,
            boolean isDeprecated,
            boolean isReadOnly
    ) { }

    public record Type(
            String name,
            String category,
            boolean isAnonymousUnionType,
            boolean isInclusion,
            boolean isArrayType,
            boolean isNullable,
            boolean isTuple,
            boolean isIntersectionType,
            boolean isParenthesisedType,
            boolean isTypeDesc,
            boolean isRestParam,
            boolean isDeprecated,
            boolean isPublic,
            boolean generateUserDefinedTypeLink,
            List<Type> memberTypes,
            int arrayDimensions,
            String orgName,
            String moduleName,
            String version
    ) { }

    public record Field() { }
}

