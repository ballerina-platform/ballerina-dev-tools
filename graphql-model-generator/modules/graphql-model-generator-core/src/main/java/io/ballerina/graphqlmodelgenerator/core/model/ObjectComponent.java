package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.ObjectKind;
import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

// represents services classes and records (in graphql - objects and inputObjects)
public class ObjectComponent {
    private final ObjectKind type; // service/ record
    private final boolean isInputObject; // type is an inputObj
    private final Position position;
    private final List<Field> fields; // could be resource remote functions, or record fields

    public ObjectComponent(ObjectKind type, boolean isInputObject, Position position, List<Field> fields) {
        this.type = type;
        this.isInputObject = isInputObject;
        this.position = position;
        this.fields = fields;
    }

    public ObjectKind getType() {
        return type;
    }

    public boolean isInputObject() {
        return isInputObject;
    }

    public Position getPosition() {
        return position;
    }

    public List<Field> getFields() {
        return fields;
    }
}
