package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.graphqlmodelgenerator.core.model.*;
import io.ballerina.graphqlmodelgenerator.core.model.EnumComponent;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.ObjectKind;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.stdlib.graphql.commons.types.Type;
import io.ballerina.stdlib.graphql.commons.types.TypeKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.graphqlmodelgenerator.core.model.DefaultIntrospectionType.isReservedType;

public class InteractedComponentModelGenerator {
    private final Schema schemaObj;
    private final Map<String, RecordComponent> records;
    private final Map<String, ServiceClassComponent> serviceClasses;
    private final Map<String, EnumComponent> enums;
    private final Map<String, UnionComponent> unions;

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

    public InteractedComponentModelGenerator(Schema schema){
        this.schemaObj = schema;
        this.records = new HashMap<>();
        this.serviceClasses = new HashMap<>();
        this.enums = new HashMap<>();
        this.unions = new HashMap<>();
    }

    public void generate(){
        for (var entry: schemaObj.getTypes().entrySet()){
            if ((entry.getValue().getKind() == TypeKind.OBJECT || entry.getValue().getKind() == TypeKind.INPUT_OBJECT ||
                    entry.getValue().getKind() == TypeKind.ENUM || entry.getValue().getKind() == TypeKind.UNION) &&
           !isReservedType(entry.getKey())) {

                if (entry.getValue().getObjectKind() == ObjectKind.RECORD &&
                        (entry.getValue().getKind() == TypeKind.OBJECT ||
                                entry.getValue().getKind() == TypeKind.INPUT_OBJECT)) {
                    this.records.put(entry.getValue().getName(), generateRecordComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.OBJECT) {
                    this.serviceClasses.put(entry.getValue().getName(), generateServiceClassComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.ENUM) {
                    this.enums.put(entry.getValue().getName(), generateEnumComponent(entry.getValue()));
                } else if (entry.getValue().getKind() == TypeKind.UNION) {
                    this.unions.put(entry.getValue().getName(), generateUnionComponent(entry.getValue()));
                }
            }
        }
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
                if (paramType.getKind().equals(TypeKind.INPUT_OBJECT)){
                    String inputObj = ModelGenerationUtils.getFieldType(paramType);
                    if (inputObj != null){
                        interactionList.add(new Interaction(inputObj, ModelGenerationUtils.getPathOfFieldType(paramType)));
                    }
                }
            });

            ServiceClassField classField = new ServiceClassField(field.getName(),typeDesc,
                    field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), params, interactionList);
            functions.add(classField);

        });
        ServiceClassComponent classComponent = new ServiceClassComponent(objType.getName(), objType.getPosition(), objType.getDescription(), functions);
        return classComponent;
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

        RecordComponent recordComponent = new RecordComponent(objType.getName(), objType.getPosition(),
                objType.getDescription(), recordFields, objType.getKind() == TypeKind.INPUT_OBJECT );
        return recordComponent;

    }

//    private ObjectComponent generateObjectComponent(Type objType) {
//        if (objType.getObjectKind() == ObjectKind.RECORD) {
//            List<RecordField> recordFields = new ArrayList<>();
//            objType.getFields().forEach(field -> {
//                String typeDesc = ModelGenerationUtils.getFormattedFieldType(field.getType());
//                List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
//                RecordField recordField = new RecordField(field.getName(), typeDesc, "",
//                        field.getDescription(), field.isDeprecated(), field.getDeprecationReason(), interactionList);
//                recordFields.add(recordField);
//            });
//
//            RecordComponent recordComponent = new RecordComponent(objType.getName(), objType.getPosition(),
//                    objType.getDescription(), recordFields, objType.getKind() == TypeKind.INPUT_OBJECT );
//        }
//
//        List<Field> fields = new ArrayList<>();
//        if (objType.getKind() == TypeKind.OBJECT){
//            objType.getFields().forEach((field) -> {
//                String returnType = ModelGenerationUtils.getFormattedFieldType(field.getType());
//                List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
//                Field objField = new Field(field.getName(),returnType,field.getDescription(),field.isDeprecated(), field.getDeprecationReason(),interactionList,null);
//                fields.add(objField);
//
//            });
//        } else {
//            objType.getInputFields().forEach((field) -> {
//                String returnType = ModelGenerationUtils.getFormattedFieldType(field.getType());
//                List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
//                Field objField = new Field(field.getName(),returnType,field.getDescription(),interactionList);
//                fields.add(objField);
//
//            });
//        }
//
//
//        ObjectComponent objectComponent = new ObjectComponent(objType.getKind() == TypeKind.INPUT_OBJECT ? ObjectKind.RECORD : objType.getObjectKind(),
//                objType.getKind() == TypeKind.INPUT_OBJECT, objType.getPosition(), fields);
//        return objectComponent;
//    }

    private EnumComponent generateEnumComponent(Type objType) {
        List<EnumField> enumFields = new ArrayList<>();
        objType.getEnumValues().forEach(enumValue -> {
            enumFields.add(new EnumField(enumValue.getName(), enumValue.getDescription(), enumValue.isDeprecated(), enumValue.getDeprecationReason()));
        });
        EnumComponent enumComponent = new EnumComponent(objType.getName(), objType.getPosition(), objType.getDescription(), enumFields);
        return enumComponent;

    }

    private UnionComponent generateUnionComponent(Type objType) {
        List<Interaction> possibleTypes = new ArrayList<>();
        objType.getPossibleTypes().forEach(type -> {
            possibleTypes.add(new Interaction(type.getName(), type.getPosition().getFilePath()));
        });
        UnionComponent unionComponent = new UnionComponent(objType.getName(), objType.getPosition(),
                objType.getDescription(), possibleTypes);
        return unionComponent;
    }

//    private boolean isComponentPresent(TypeKind typeKind){
//        boolean isFound = false;
//        for (var entry: schemaObj.getTypes().entrySet()) {
//            if (entry.getValue().getKind() == typeKind && !isReservedType(entry.getValue().getName())){
//                isFound = true;
//                break;
//            }
//        }
//        return isFound;
//    }
}
