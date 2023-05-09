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

package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.graphqlmodelgenerator.core.model.Interaction;
import io.ballerina.graphqlmodelgenerator.core.model.Param;
import io.ballerina.graphqlmodelgenerator.core.model.RemoteFunction;
import io.ballerina.graphqlmodelgenerator.core.model.ResourceFunction;
import io.ballerina.graphqlmodelgenerator.core.model.Service;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.LinePosition;
import io.ballerina.stdlib.graphql.commons.types.Position;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.stdlib.graphql.commons.types.Type;
import io.ballerina.stdlib.graphql.commons.types.TypeKind;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the model generator involved in graphQL service including the graphQL operations.
 *
 * @since 2201.5.0
 */
public class ServiceModelGenerator {
    private final Schema schemaObj;
    private final String serviceName;
    private final List<ResourceFunction> resourceFunctions;
    private final List<RemoteFunction> remoteFunctions;
    private final LineRange servicePosition;
    private final SyntaxTree syntaxTree;

    public ServiceModelGenerator(Schema schema, String serviceName, LineRange servicePosition, SyntaxTree syntaxTree) {
        this.schemaObj = schema;
        this.serviceName = serviceName;
        this.resourceFunctions = new ArrayList<>();
        this.remoteFunctions = new ArrayList<>();
        this.servicePosition = servicePosition;
        this.syntaxTree = syntaxTree;
    }

    enum OperationKind {
        QUERY,
        MUTATION,
        SUBSCRIPTION
    }

    public Service generate() {
        generateGraphqlOperation(schemaObj.getQueryType(), OperationKind.QUERY);
        generateGraphqlOperation(schemaObj.getMutationType(), OperationKind.MUTATION);
        generateGraphqlOperation(schemaObj.getSubscriptionType(), OperationKind.SUBSCRIPTION);

        Position nodePosition = new Position(servicePosition.fileName(),
                new LinePosition(servicePosition.startLine().line(), servicePosition.startLine().offset()),
                new LinePosition(servicePosition.endLine().line(), servicePosition.endLine().offset()));

        return new Service(serviceName, nodePosition, schemaObj.getDescription(), resourceFunctions, remoteFunctions);
    }

    private void generateGraphqlOperation(Type operation, OperationKind operationKind) {
        if (operation != null) {
            operation.getFields().forEach(field -> {
                String returns = ModelGenerationUtils.getFormattedFieldType(field.getType());
                List<Interaction> links = ModelGenerationUtils.getInteractionList(field);
                List<Param> params = new ArrayList<>();
                field.getArgs().forEach(inputValue -> {
                    Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                            inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                    params.add(param);
                    Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                    if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)) {
                        String inputObj = ModelGenerationUtils.getFieldType(paramType);
                        if (inputObj != null) {
                            links.add(new Interaction(inputObj, ModelGenerationUtils.getPathOfFieldType(paramType)));
                        }
                    }
                });
                Position position = ModelGenerationUtils.findNodeRange(field.getPosition(), this.syntaxTree);
                if (operationKind == OperationKind.QUERY) {
                    ResourceFunction resourceFunction = new ResourceFunction(field.getName(), false, returns,
                            position, field.getDescription(), field.isDeprecated(), field.getDeprecationReason(),
                            params, links);
                    resourceFunctions.add(resourceFunction);
                } else if (operationKind == OperationKind.MUTATION) {
                    RemoteFunction remoteFunction = new RemoteFunction(field.getName(), returns, position,
                            field.getDescription(),
                            field.isDeprecated(), field.getDeprecationReason(), params, links);
                    remoteFunctions.add(remoteFunction);
                } else if (operationKind == OperationKind.SUBSCRIPTION) {
                    ResourceFunction resourceFunction = new ResourceFunction(field.getName(), true, returns,
                            position, field.getDescription(), field.isDeprecated(), field.getDeprecationReason(),
                            params, links);
                    resourceFunctions.add(resourceFunction);
                }
            });
        }
    }
}
