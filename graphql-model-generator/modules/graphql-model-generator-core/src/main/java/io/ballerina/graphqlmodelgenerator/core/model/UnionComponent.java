package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class UnionComponent {
    private final String name;
    private final Position position;
    private final List<Interaction> possibleTypes;

    public UnionComponent(String name, Position position, List<Interaction> possibleTypes) {
        this.name = name;
        this.position = position;
        this.possibleTypes = possibleTypes;
    }
}
