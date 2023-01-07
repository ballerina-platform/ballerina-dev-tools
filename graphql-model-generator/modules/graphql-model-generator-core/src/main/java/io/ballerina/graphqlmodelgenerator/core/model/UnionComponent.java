package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class UnionComponent {
    private final String name;
    private final List<Interaction> possibleTypes;

    public UnionComponent(String name, List<Interaction> possibleTypes) {
        this.name = name;
        this.possibleTypes = possibleTypes;
    }
}
