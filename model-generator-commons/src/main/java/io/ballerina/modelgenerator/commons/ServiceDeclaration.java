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

/**
 * Represents a service declaration.
 * @param packageInfo The package information containing organization, name and version
 * @param displayName The display name of the service
 * @param optionalTypeDescriptor The optional type descriptor
 * @param typeDescriptorLabel The label of the type descriptor
 * @param typeDescriptorDescription The description of the type descriptor
 * @param typeDescriptorDefaultValue The default value of the type descriptor
 * @param addDefaultTypeDescriptor The add default type descriptor
 * @param optionalAbsoluteResourcePath The optional absolute resource path
 * @param absoluteResourcePathLabel The label of the absolute resource path
 * @param absoluteResourcePathDescription The description of the absolute resource path
 * @param absoluteResourcePathDefaultValue The default value of the absolute resource path
 * @param optionalStringLiteral The optional string literal
 * @param stringLiteralLabel The label of the string literal
 * @param stringLiteralDescription The description of the string literal
 * @param stringLiteralDefaultValue The default value of the string literal
 * @param listenerKind The kind of the listener
 * @param kind The kind of the service
 *
 * @since 2.0.0
 */
public record ServiceDeclaration(Package packageInfo, String displayName,
                                 int optionalTypeDescriptor, String typeDescriptorLabel,
                                 String typeDescriptorDescription, String typeDescriptorDefaultValue,
                                 int addDefaultTypeDescriptor, int optionalAbsoluteResourcePath,
                                 String absoluteResourcePathLabel, String absoluteResourcePathDescription,
                                 String absoluteResourcePathDefaultValue, int optionalStringLiteral,
                                 String stringLiteralLabel, String stringLiteralDescription,
                                 String stringLiteralDefaultValue, String listenerKind, String kind) {

    public record Package(int packageId, String org, String name, String version) {
    }
}
