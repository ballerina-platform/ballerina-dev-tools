package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

// represents services classes and records (in graphql - objects and inputObjects)
public class ObjectComponent {
    private final ObjectType type; // service/ record
    private final boolean isInputObject; // type is an inputObj
    private final List<Field> fields; // could be resource remote functions, or record fields

    public ObjectComponent(ObjectType type, boolean isInputObject, List<Field> fields) {
        this.type = type;
        this.isInputObject = isInputObject;
        this.fields = fields;
    }
}
