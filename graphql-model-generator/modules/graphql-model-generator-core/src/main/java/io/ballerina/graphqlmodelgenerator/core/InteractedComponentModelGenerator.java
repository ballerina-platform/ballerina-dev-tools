package io.ballerina.graphqlmodelgenerator.core;

import io.ballerina.graphqlmodelgenerator.core.model.*;
import io.ballerina.graphqlmodelgenerator.core.model.EnumComponent;
import io.ballerina.graphqlmodelgenerator.core.utils.ModelGenerationUtils;
import io.ballerina.stdlib.graphql.commons.types.Schema;
import io.ballerina.stdlib.graphql.commons.types.Type;
import io.ballerina.stdlib.graphql.commons.types.TypeKind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractedComponentModelGenerator {
    private final Schema schemaObj;
    private final Map<String, ObjectComponent> objects;
    private final Map<String, EnumComponent> enums;
    private final Map<String, UnionComponent> unions;
    private List<Interaction> linkedComponents;

    public Map<String, EnumComponent> getEnums() {
        return enums;
    }

    public Map<String, UnionComponent> getUnions() {
        return unions;
    }

    public Map<String, ObjectComponent> getObjects() {
        return objects;
    }

    public InteractedComponentModelGenerator(Schema schema, List<Interaction> linkedComponents){
        this.schemaObj = schema;
        this.linkedComponents = linkedComponents;
        this.objects = isComponentPresent(TypeKind.OBJECT) || isComponentPresent(TypeKind.INPUT_OBJECT) ? new HashMap<>() : null;
        this.enums = isComponentPresent(TypeKind.ENUM) ? new HashMap<>() : null;
        this.unions = isComponentPresent(TypeKind.UNION) ? new HashMap<>() : null;

    }

    public void generate(){
        for (var entry: schemaObj.getTypes().entrySet()){
            linkedComponents.forEach(component -> {
                if (component.getComponentName().equals(entry.getKey())) {
                    if (entry.getValue().getKind() == TypeKind.OBJECT || entry.getValue().getKind() == TypeKind.INPUT_OBJECT ){
                        this.objects.put(entry.getValue().getName(), generateObjectComponent(entry.getValue()));
                    } else if (entry.getValue().getKind() == TypeKind.ENUM) {
                        this.enums.put(entry.getValue().getName(), generateEnumComponent(entry.getValue()));
                    } else if (entry.getValue().getKind() == TypeKind.UNION) {
                        this.unions.put(entry.getValue().getName(), generateUnionComponent(entry.getValue()));
                    }
                }
            });
        }
    }

    private ObjectComponent generateObjectComponent(Type objType) {
        List<Field> fields = new ArrayList<>();
        objType.getFields().forEach((field) -> {
            List<String> returnTypes = ModelGenerationUtils.getFormattedFieldTypeList(field);
            List<Interaction> interactionList = ModelGenerationUtils.getInteractionList(field);
//            List<String> returns = new ArrayList<>();
//            returns.add(ModelGenerationUtils.getFormattedFieldType(field.getType()));
//            List<Interaction> links = new ArrayList<>();
//            String link = ModelGenerationUtils.getFieldType(field.getType());
//            if (link != null){
//                links.add(new Interaction(link));
//            }
            Field objField = new Field(field.getName(),returnTypes,field.getDescription(),field.isDeprecated(), field.getDeprecationReason(),interactionList,null);
            fields.add(objField);

        });
        ObjectComponent objectComponent = new ObjectComponent(ObjectType.SERVICE,objType.getKind() == TypeKind.INPUT_OBJECT,fields);
        return objectComponent;
    }

    private EnumComponent generateEnumComponent(Type objType) {
        List<EnumField> enumFields = new ArrayList<>();
        objType.getEnumValues().forEach(enumValue -> {
            enumFields.add(new EnumField(enumValue.getName(), enumValue.getDescription(), enumValue.isDeprecated(), enumValue.getDeprecationReason()));
        });
        EnumComponent enumComponent = new EnumComponent(objType.getName(), enumFields);
        return enumComponent;

    }

    private UnionComponent generateUnionComponent(Type objType) {
        List<Interaction> possibleTypes = new ArrayList<>();
        objType.getPossibleTypes().forEach(type -> {
            possibleTypes.add(new Interaction(type.getName()));
        });
        UnionComponent unionComponent = new UnionComponent(objType.getName(),possibleTypes);
        return unionComponent;
    }

    private boolean isComponentPresent(TypeKind typeKind){
        boolean isFound = false;
        for (var entry: schemaObj.getTypes().entrySet()) {
            if (entry.getValue().getKind() == typeKind){
                isFound = true;
                break;
            }
        }
        return isFound;
    }
}
