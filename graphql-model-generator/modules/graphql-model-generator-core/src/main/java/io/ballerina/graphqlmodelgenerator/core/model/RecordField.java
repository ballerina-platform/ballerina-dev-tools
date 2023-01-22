package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class RecordField {
    private final String name;
    private final String type;
    private final String defaultValue;
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;
    private final List<Interaction> interactions;

    public RecordField(String name, String type, String defaultValue, String description, boolean isDeprecated, String deprecationReason, List<Interaction> interactions) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
        this.interactions = interactions;
    }
}
