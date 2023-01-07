package io.ballerina.graphqlmodelgenerator.core.model;

public class EnumField {
    private final String name;
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;

    public EnumField(String name, String description) {
        this(name, description, false, null);
    }

    public EnumField(String name, String description, boolean isDeprecated, String deprecationReason) {
        this.name = name;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
    }
}
