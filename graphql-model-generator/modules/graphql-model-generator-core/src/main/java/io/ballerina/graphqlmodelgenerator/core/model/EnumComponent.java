package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class EnumComponent {
    private final String name;
    private final Position position;
    private final List<EnumField> enumFields;

    public EnumComponent(String name, Position position, List<EnumField> enumFields) {
        this.name = name;
        this.position = position;
        this.enumFields = enumFields;
    }
}
