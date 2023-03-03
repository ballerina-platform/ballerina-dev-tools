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

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

/**
 * Represents the resource function of the graphQL service.
 *
 * @since 2201.5.0
 */
public class ResourceFunction {
    private final String identifier;
    private final boolean subscription;
    private final String returns;
    private final Position position;
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;
    private List<Param> parameters;
    private List<Interaction> interactions;

    public ResourceFunction(String identifier, boolean subscription, String returns, Position position,
                            String description, boolean isDeprecated, String deprecationReason, List<Param> parameters,
                            List<Interaction> interactions) {
        this.identifier = identifier;
        this.subscription = subscription;
        this.returns = returns;
        this.position = position;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
        this.parameters = parameters;
        this.interactions = interactions;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public String getReturns() {
        return returns;
    }

    public Position getPosition() {
        return position;
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

    public List<Param> getParameters() {
        return parameters;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }
}
