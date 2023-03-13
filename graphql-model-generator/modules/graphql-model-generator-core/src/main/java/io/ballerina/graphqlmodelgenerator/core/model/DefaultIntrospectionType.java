/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the Default Introspection types.
 *
 * @since 2201.5.0
 */
public enum DefaultIntrospectionType {
    STRING("String"),
    INT("Int"),
    FLOAT("Float"),
    BOOLEAN("Boolean"),
    DECIMAL("Decimal"),
    UPLOAD("Upload"),
    SCHEMA("__Schema"),
    TYPE("__Type"),
    FIELD("__Field"),
    INPUT_VALUE("__InputValue"),
    ENUM_VALUE("__EnumValue"),
    TYPE_KIND("__TypeKind"),
    DIRECTIVE("__Directive"),
    DIRECTIVE_LOCATION("__DirectiveLocation"),
    QUERY("Query"),
    MUTATION("Mutation"),
    SUBSCRIPTION("Subscription");

    private final String name;
    private static final List<DefaultIntrospectionType> reservedIntrospectionTypes =
            Arrays.asList(DefaultIntrospectionType.values());

    public static boolean isReservedType(String typeName) {
        return reservedIntrospectionTypes.stream().anyMatch(value -> value.getName().equals(typeName));
    }

    DefaultIntrospectionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
