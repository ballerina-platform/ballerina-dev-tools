package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class InterfaceComponent {
    private final String name;
    private final Position position;
    private final String description;
    private final List<Interaction> possibleTypes;
    private final List<ResourceFunction> resourceFunctions;

    public InterfaceComponent(String name, Position position, String description, List<Interaction> possibleTypes, List<ResourceFunction> resourceFunctions) {
        this.name = name;
        this.position = position;
        this.description = description;
        this.possibleTypes = possibleTypes;
        this.resourceFunctions = resourceFunctions;
    }
}
