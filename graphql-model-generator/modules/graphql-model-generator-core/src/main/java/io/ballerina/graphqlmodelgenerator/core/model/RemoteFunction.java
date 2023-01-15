package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class RemoteFunction {
    private final String identifier;
    private final String returns;
    private List<Param> parameters;
    private List<Interaction> interactions;

    public String getIdentifier() {
        return identifier;
    }

    public String getReturns() {
        return returns;
    }

    public List<Interaction> getInteractions() {
        return interactions;
    }

    public RemoteFunction(String identifier, String returns, List<Param> parameters, List<Interaction> interactions) {
        this.identifier = identifier;
        this.returns = returns;
        this.parameters = parameters;
        this.interactions = interactions;
    }

    public RemoteFunction(String identifier, String returns,  List<Param> parameters) {
        this.identifier = identifier;
        this.returns = returns;
        this.parameters = parameters;
    }
}
