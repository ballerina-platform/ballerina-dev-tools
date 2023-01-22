package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class RecordComponent {
    private final String name;
    private final Position position;
    private final String description;
    private final List<RecordField> recordFields;
    private final boolean isInputObject;

    public RecordComponent(String name, Position position, String description, List<RecordField> recordFields, boolean isInputObject) {
        this.name = name;
        this.position = position;
        this.description = description;
        this.recordFields = recordFields;
        this.isInputObject = isInputObject;
    }
}
