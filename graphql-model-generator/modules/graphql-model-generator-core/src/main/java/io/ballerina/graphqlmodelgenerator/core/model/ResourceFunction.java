package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class ResourceFunction {
    private final String identifier;
    private final boolean subscription;
    private final String returns;
    private final String description;
    private final boolean isDeprecated;
    private final String deprecationReason;
    private List<Param> parameters;
    private List<Interaction> interactions;

    public String getIdentifier() {
        return identifier;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public String getReturns() {
        return returns;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public ResourceFunction(String identifier, boolean subscription, String returns, String description, boolean isDeprecated, String deprecationReason, List<Param> parameters, List<Interaction> interactions) {
        this.identifier = identifier;
        this.subscription = subscription;
        this.returns = returns;
        this.description = description;
        this.isDeprecated = isDeprecated;
        this.deprecationReason = deprecationReason;
        this.parameters = parameters;
        this.interactions = interactions;
    }
}
