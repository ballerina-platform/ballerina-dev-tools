package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class RemoteFunction {
    private final String identifier;
    private final List<String> returns;
    private List<Interaction> interactions;

    public RemoteFunction(String identifier, List<String> returns, List<Interaction> interactions) {
        this.identifier = identifier;
        this.returns = returns;
        this.interactions = interactions;
    }

    public RemoteFunction(String identifier, List<String> returns) {
        this.identifier = identifier;
        this.returns = returns;
    }
}
