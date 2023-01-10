package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

// function parameters
public class Param {
    private final String type;
    private final String name;
    private final String description;
    private String defaultValue;

    public Param(String type, String name, String description, String defaultValue) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;

    }
}
