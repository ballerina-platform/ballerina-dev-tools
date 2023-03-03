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

/**
 * Represents the Enum Field type of Enum Component.
 *
 * @since 2201.5.0
 */
public class EnumField {
    private final String name;
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;

    public EnumField(String name, String description, boolean isDeprecated, String deprecationReason) {
        this.name = name;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }
}
