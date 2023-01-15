package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

// remote/ resource/ record field
public class Field {
    private final String name;
    private final String type; // return type
    // private final String fieldKind; // TODO: check if we can identify the resource/ remote type
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;
    private final List<Interaction> interactions; //
    private final List<Param> parameters;

    public Field(String name, String type, String description, boolean isDeprecated, String deprecationReason, List<Interaction> interactions, List<Param> parameters) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
        this.interactions = interactions;
        this.parameters = parameters;
    }

    public Field(String name, String type, String description, List<Interaction> interactions) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.interactions = interactions;
        this.isDeprecated = false;
        this.deprecationReason = null;
        this.parameters = null;

    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeprecated() {
        return isDeprecated;
    }

    public String getDeprecationReason() {
        return deprecationReason;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public List<Param> getParameters() {
        return parameters;
    }
}
