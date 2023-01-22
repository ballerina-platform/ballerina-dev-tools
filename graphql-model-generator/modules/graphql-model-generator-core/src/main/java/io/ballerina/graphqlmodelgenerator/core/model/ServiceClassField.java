package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class ServiceClassField {
    private final String identifier;
    private final String returnType;
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;
    private List<Param> parameters;
    private List<Interaction> interactions;

    public ServiceClassField(String identifier, String returnType, String description, boolean isDeprecated, String deprecationReason, List<Param> parameters, List<Interaction> interactions) {
        this.identifier = identifier;
        this.returnType = returnType;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
        this.parameters = parameters;
        this.interactions = interactions;
    }
}
