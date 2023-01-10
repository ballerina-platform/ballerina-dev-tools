package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

// function parameters
public class Params {
    private final List<String> type;
    private final String name;
    private boolean isOptional;
    private String defaultValue;

    public Params(List<String> type, String name) {
        this.type = type;
        this.name = name;
    }

    public Params(List<String> type, String name, boolean isOptional, String defaultValue) {
        this.type = type;
        this.name = name;
        this.isOptional = isOptional;
        this.defaultValue = defaultValue;
    }
}
