package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

// remote/ resource/ record field
public class Field {
    private final String name;
    private final List<String> type; // return type
    // private final String fieldKind; // TODO: check if we can identify the resource/ remote type
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;
    private final List<Interaction> interactions; //
    private final List<Params> parameters;

    public Field(String name, List<String> type, String description, boolean isDeprecated, String deprecationReason, List<Interaction> interactions, List<Params> parameters) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
        this.interactions = interactions;
        this.parameters = parameters;
    }
}
