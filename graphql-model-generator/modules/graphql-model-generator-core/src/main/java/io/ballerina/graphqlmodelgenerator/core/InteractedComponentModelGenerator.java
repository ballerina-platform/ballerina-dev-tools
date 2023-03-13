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
import io.ballerina.graphqlmodelgenerator.core.model.EnumComponent;
import io.ballerina.graphqlmodelgenerator.core.model.EnumField;
import io.ballerina.graphqlmodelgenerator.core.model.HierarchicalResourceComponent;
import io.ballerina.graphqlmodelgenerator.core.model.Interaction;
import io.ballerina.graphqlmodelgenerator.core.model.InterfaceComponent;
import io.ballerina.graphqlmodelgenerator.core.model.Param;
import io.ballerina.graphqlmodelgenerator.core.model.RecordComponent;
import io.ballerina.graphqlmodelgenerator.core.model.RecordField;
import io.ballerina.graphqlmodelgenerator.core.model.ResourceFunction;
import io.ballerina.graphqlmodelgenerator.core.model.ServiceClassComponent;
import io.ballerina.graphqlmodelgenerator.core.model.ServiceClassField;
import io.ballerina.graphqlmodelgenerator.core.model.UnionComponent;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.ObjectKind;
import io.ballerina.stdlib.graphql.commons.types.Position;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.stdlib.graphql.commons.types.Type;
import io.ballerina.stdlib.graphql.commons.types.TypeKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.graphqlmodelgenerator.core.model.DefaultIntrospectionType.isReservedType;

/**
 * Represents the model generator used for Interacted components of graphQL operations.
 *
 * @since 2201.5.0
 */
public class InteractedComponentModelGenerator {
    private final Schema schemaObj;
    private final SyntaxTree syntaxTree;
    private final Map<String, RecordComponent> records;
    private final Map<String, ServiceClassComponent> serviceClasses;
    private final Map<String, EnumComponent> enums;
    private final Map<String, UnionComponent> unions;
    private final Map<String, InterfaceComponent> interfaces;
    private final Map<String, HierarchicalResourceComponent> hierarchicalResources;

    public InteractedComponentModelGenerator(Schema schema, SyntaxTree syntaxTree) {
        this.schemaObj = schema;
        this.syntaxTree = syntaxTree;
        this.records = new HashMap<>();
        this.serviceClasses = new HashMap<>();
        this.enums = new HashMap<>();
        this.unions = new HashMap<>();
        this.interfaces = new HashMap<>();
        this.hierarchicalResources = new HashMap<>();
    }

    public Map<String, RecordComponent> getRecords() {
        return records;
    }

    public Map<String, ServiceClassComponent> getServiceClasses() {
        return serviceClasses;
    }

    public Map<String, EnumComponent> getEnums() {
        return enums;
    }

    public Map<String, UnionComponent> getUnions() {
        return unions;
    }

    public Map<String, InterfaceComponent> getInterfaces() {
        return interfaces;
    }

    public Map<String, HierarchicalResourceComponent> getHierarchicalResources() {
        return hierarchicalResources;
    }

    public void generate() {
        for (var entry : schemaObj.getTypes().entrySet()) {
            if ((entry.getValue().getKind() == TypeKind.OBJECT || entry.getValue().getKind() == TypeKind.INPUT_OBJECT ||
                    entry.getValue().getKind() == TypeKind.ENUM || entry.getValue().getKind() == TypeKind.UNION ||
                    entry.getValue().getKind() == TypeKind.INTERFACE) &&
                    !isReservedType(entry.getKey())) {

                if (entry.getValue().getObjectKind() == ObjectKind.RECORD ||
                        entry.getValue().getKind() == TypeKind.INPUT_OBJECT) {
                    this.records.put(entry.getValue().getName(), generateRecordComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.OBJECT &&
                        entry.getValue().getObjectKind() == ObjectKind.CLASS) {
                    this.serviceClasses.put(entry.getValue().getName(),
                            generateServiceClassComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.ENUM) {
                    this.enums.put(entry.getValue().getName(), generateEnumComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.UNION && !entry.getValue().getName().isBlank()) {
                    this.unions.put(entry.getValue().getName(), generateUnionComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.INTERFACE) {
                    this.interfaces.put(entry.getValue().getName(), generateInterfaceComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.OBJECT) {
                    this.hierarchicalResources.put(entry.getValue().getName(),
                            generateHierarchicalResourceComponent(entry.getValue()));
                }
            }
        }
    }

    private HierarchicalResourceComponent generateHierarchicalResourceComponent(Type objType) {
        List<ResourceFunction> resourceFunctions = new ArrayList<>();
        objType.getFields().forEach(field -> {
            String typeDesc = ModelGenerationUtils.getFormattedFieldType(field.getType());
            List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
            List<Param> params = new ArrayList<>();
            field.getArgs().forEach(inputValue -> {
                Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                        inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                params.add(param);
                Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)) {
                    String inputObj = ModelGenerationUtils.getFieldType(paramType);
                    if (inputObj != null) {
                        interactionList.add(new Interaction(inputObj,
                                ModelGenerationUtils.getPathOfFieldType(paramType)));
                    }
                }
            });
            Position position = ModelGenerationUtils.findNodeRange(field.getPosition(), this.syntaxTree);
            ResourceFunction resourceFunction = new ResourceFunction(field.getName(), false, typeDesc,
                    position, field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), params,
                    interactionList);
            resourceFunctions.add(resourceFunction);

        });

        return new HierarchicalResourceComponent(objType.getName(), resourceFunctions);
    }

    private InterfaceComponent generateInterfaceComponent(Type objType) {
        List<Interaction> possibleTypes = new ArrayList<>();
        objType.getPossibleTypes().forEach(type -> {
            possibleTypes.add(new Interaction(type.getName(), type.getPosition().getFilePath()));
        });
        List<ResourceFunction> resourceFunctions = new ArrayList<>();
        objType.getFields().forEach(field -> {
            String typeDesc = ModelGenerationUtils.getFormattedFieldType(field.getType());
            List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
            List<Param> params = new ArrayList<>();
            field.getArgs().forEach(inputValue -> {
                Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                        inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                params.add(param);
                Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)) {
                    String inputObj = ModelGenerationUtils.getFieldType(paramType);
                    if (inputObj != null) {
                        interactionList.add(new Interaction(inputObj,
                                ModelGenerationUtils.getPathOfFieldType(paramType)));
                    }
                }
            });
            ResourceFunction resourceFunction = new ResourceFunction(field.getName(), false, typeDesc,
                    null, field.getDescription(),
                    field.isDeprecated(), field.getDeprecationReason(), params, interactionList);
            resourceFunctions.add(resourceFunction);
        });

        return new InterfaceComponent(objType.getName(), objType.getPosition(), objType.getDescription(),
                possibleTypes, resourceFunctions);
    }

    private ServiceClassComponent generateServiceClassComponent(Type objType) {
        List<ServiceClassField> functions = new ArrayList<>();
        objType.getFields().forEach(field -> {

            String typeDesc = ModelGenerationUtils.getFormattedFieldType(field.getType());
            List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
            List<Param> params = new ArrayList<>();
            field.getArgs().forEach(inputValue -> {
                Param param = new Param(ModelGenerationUtils.createArgType(inputValue),
                        inputValue.getName(), inputValue.getDescription(), inputValue.getDefaultValue());
                params.add(param);
                Type paramType = ModelGenerationUtils.getType(inputValue.getType());
                if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)) {
                    String inputObj = ModelGenerationUtils.getFieldType(paramType);
                    if (inputObj != null) {
                        interactionList.add(new Interaction(inputObj,
                                ModelGenerationUtils.getPathOfFieldType(paramType)));
                    }
                }
            });

            ServiceClassField classField = new ServiceClassField(field.getName(), typeDesc,
                    field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), params,
                    interactionList);
            functions.add(classField);

        });
        return new ServiceClassComponent(objType.getName(), objType.getPosition(),
                objType.getDescription(), functions);
    }

    private RecordComponent generateRecordComponent(Type objType) {
        List<RecordField> recordFields = new ArrayList<>();
        if (objType.getKind() == TypeKind.OBJECT) {
            objType.getFields().forEach(field -> {
                String typeDesc = ModelGenerationUtils.getFormattedFieldType(field.getType());
                List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
                RecordField recordField = new RecordField(field.getName(), typeDesc, null,
                        field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), interactionList);
                recordFields.add(recordField);
            });
        }
        if (objType.getKind() == TypeKind.INPUT_OBJECT) {
            objType.getInputFields().forEach(field -> {
                String typeDesc = ModelGenerationUtils.getFormattedFieldType(field.getType());
                List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
                RecordField recordField = new RecordField(field.getName(), typeDesc, field.getDefaultValue(),
                        field.getDescription(), false, null, interactionList);
                recordFields.add(recordField);
            });
        }

        return new RecordComponent(objType.getName(), objType.getPosition(),
                objType.getDescription(), recordFields, objType.getKind() == TypeKind.INPUT_OBJECT);
    }

    private EnumComponent generateEnumComponent(Type objType) {
        List<EnumField> enumFields = new ArrayList<>();
        objType.getEnumValues().forEach(enumValue -> {
            enumFields.add(new EnumField(enumValue.getName(), enumValue.getDescription(), enumValue.isDeprecated(),
                    enumValue.getDeprecationReason()));
        });
        return new EnumComponent(objType.getName(), objType.getPosition(),
                objType.getDescription(), enumFields);
    }

    private UnionComponent generateUnionComponent(Type objType) {
        List<Interaction> possibleTypes = new ArrayList<>();
        objType.getPossibleTypes().forEach(type -> {
            possibleTypes.add(new Interaction(type.getName(), type.getPosition().getFilePath()));
        });
        return new UnionComponent(objType.getName(), objType.getPosition(),
                objType.getDescription(), possibleTypes);
    }
}
