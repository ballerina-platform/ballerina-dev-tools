package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;
import java.util.Map;

public class ResourceFunction {
    private final String identifier;
    private final boolean subscription;
    private final List<String> returns;
    private List<Interaction> interactions;

    public String getIdentifier() {
        return identifier;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public List<String> getReturns() {
        return returns;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public ResourceFunction(String identifier, boolean subscription, List<String> returns) {
        this.identifier = identifier;
        this.subscription = subscription;
        this.returns = returns;
    }

    public ResourceFunction(String identifier, boolean subscription, List<String> returns, List<Interaction> interactions) {
        this.identifier = identifier;
        this.subscription = subscription;
        this.returns = returns;
        this.interactions = interactions;
    }
}
