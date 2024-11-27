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

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a response from a connector.
 *
 * @param id                The unique identifier of the connector.
 * @param name              The name of the connector.
 * @param displayName       The display name of the connector.
 * @param documentation     The documentation for the connector.
 * @param moduleName        The module name of the connector.
 * @param functions         The list of functions provided by the connector.
 * @param displayAnnotation The display annotation for the connector.
 * @param packageInfo       The package information of the connector.
 * @param icon              The icon representing the connector.
 * @since 2.0.0
 */
public record ConnectorResponse(
        int id,
        String name,
        String displayName,
        String documentation,
        String moduleName,
        List<Function> functions,
        DisplayAnnotation displayAnnotation,
        @SerializedName("package") PackageInfo packageInfo,
        String icon
) {

    public record Function(
            List<String> qualifiers,
            String documentation,
            String name,
            List<PathParam> pathParams,
            List<Parameter> parameters,
            ReturnType returnType,
            DisplayAnnotation displayAnnotation
    ) { }

    public record PathParam() { }

    public record Parameter(
            String name,
            String typeName,
            boolean optional,
            TypeInfo typeInfo,
            DisplayAnnotation displayAnnotation,
            String documentation,
            boolean hasRestType,
            boolean defaultable,
            List<Field> fields,
            String defaultValue,
            InclusionType inclusionType
    ) { }

    public record Field(
            String name,
            String typeName,
            boolean optional,
            boolean defaultable,
            List<Field> fields,
            boolean hasRestType,
            TypeInfo typeInfo,
            List<Member> members,
            MemberType memberType
    ) { }

    public record Member(
            String typeName,
            boolean optional,
            boolean defaultable
    ) { }

    public record MemberType(
            String typeName,
            boolean optional,
            boolean defaultable
    ) { }

    public record TypeInfo(
            String name,
            String orgName,
            String moduleName,
            String version
    ) { }

    public record ReturnType(
            String typeName,
            boolean optional,
            DisplayAnnotation displayAnnotation,
            String documentation,
            boolean defaultable
    ) { }

    public record DisplayAnnotation(
            String label,
            String iconPath
    ) { }

    public record PackageInfo(
            int id,
            String organization,
            String name,
            String version,
            String platform,
            String languageSpecificationVersion,
            boolean isDeprecated,
            String deprecateMessage,
            String URL,
            String balaVersion,
            String balaURL,
            String digest,
            String summary,
            String readme,
            boolean template,
            List<String> licenses,
            List<String> authors,
            String sourceCodeLocation,
            List<String> keywords,
            String ballerinaVersion,
            String icon,
            String ownerUUID,
            long createdDate,
            int pullCount,
            String visibility,
            List<Module> modules,
            String balToolId,
            String graalvmCompatible
    ) { }

    public record InclusionType(
            boolean hasRestType,
            String name,
            String typeName,
            boolean optional,
            TypeInfo typeInfo,
            boolean defaultable,
            List<Field> fields
    ) { }

    public record Module() { }
}
