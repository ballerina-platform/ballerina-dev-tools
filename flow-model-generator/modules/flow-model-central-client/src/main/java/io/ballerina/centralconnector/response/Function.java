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
 * Represents a function with API doc attributes.
 *
 * @param accessor              The accessor of the function.
 * @param resourcePath          The resource path of the function.
 * @param isIsolated            Indicates if the function is isolated.
 * @param isRemote              Indicates if the function is remote.
 * @param isResource            Indicates if the function is a resource.
 * @param isExtern              Indicates if the function is external.
 * @param parameters            The list of parameters of the function.
 * @param returnParameters      The list of return parameters of the function.
 * @param annotationAttachments The list of annotation attachments of the function.
 * @param name                  The name of the function.
 * @param description           The description of the function.
 * @param descriptionSections   The list of description sections of the function.
 * @param isDeprecated          Indicates if the function is deprecated.
 * @param isReadOnly            Indicates if the function is read-only.
 * @since 2.0.0
 */
public record Function(
        String accessor,
        String resourcePath,
        boolean isIsolated,
        boolean isRemote,
        boolean isResource,
        boolean isExtern,
        List<Parameter> parameters,
        List<ReturnParameter> returnParameters,
        List<AnnotationAttachment> annotationAttachments,
        String name,
        String description,
        List<DescriptionSection> descriptionSections,
        boolean isDeprecated,
        boolean isReadOnly
) {

    public record Parameter(
            String defaultValue,
            List<AnnotationAttachment> annotationAttachments,
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

    public record AnnotationAttachment() { }

    public record DescriptionSection() { }

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
            ElementType elementType
    ) { }

    public record ElementType(
            String orgName,
            String moduleName,
            String version,
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
            int arrayDimensions
    ) { }
}
