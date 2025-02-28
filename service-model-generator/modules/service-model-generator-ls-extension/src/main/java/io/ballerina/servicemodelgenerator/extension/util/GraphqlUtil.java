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

package io.ballerina.servicemodelgenerator.extension.util;

import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;

/**
 * Util class for GraphQL related operations.
 *
 * @since 2.0.0
 */
public class GraphqlUtil {

    public static void updateGraphqlFunctionMetaData(Function function) {
        switch (function.getKind()) {
            case ServiceModelGeneratorConstants.KIND_QUERY -> {
                function.setMetadata(graphqlQueryMetaData());
                function.getName().setMetadata(graphqlQueryNameMetaData());
            }
            case ServiceModelGeneratorConstants.KIND_MUTATION -> {
                function.setMetadata(graphqlMutationMetaData());
                function.getName().setMetadata(graphqlMutationNameMetaData());
            }
            case ServiceModelGeneratorConstants.KIND_SUBSCRIPTION -> {
                function.setMetadata(graphqlSubscriptionMetaData());
                function.getName().setMetadata(graphqlSubscriptionNameMetaData());
            }
            default -> { }
        }
    }

    private static MetaData graphqlSubscriptionNameMetaData() {
        return new MetaData("Subscription Name", "The name of the subscription");
    }

    private static MetaData graphqlQueryNameMetaData() {
        return new MetaData("Field Name", "The name of the field");
    }

    private static MetaData graphqlMutationNameMetaData() {
        return new MetaData("Mutation Name", "The name of the mutation");
    }

    private static MetaData graphqlSubscriptionMetaData() {
        return new MetaData("Graphql Subscription", "Graphql Subscription");
    }

    private static MetaData graphqlQueryMetaData() {
        return new MetaData("Graphql Query", "Graphql Query");
    }

    private static MetaData graphqlMutationMetaData() {
        return new MetaData("Graphql Mutation", "Graphql Mutation");
    }
}
