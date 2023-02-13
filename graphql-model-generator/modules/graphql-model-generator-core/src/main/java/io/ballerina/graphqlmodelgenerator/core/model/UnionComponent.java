package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class UnionComponent {
    private final String name;
    private final Position position;
    private final String description;
    private final List<Interaction> possibleTypes;

    public UnionComponent(String name, Position position, String description, List<Interaction> possibleTypes) {
        this.name = name;
        this.position = position;
        this.description = description;
        this.possibleTypes = possibleTypes;
    }
}