package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class Service {
    private final String serviceName;
    private final Position position;
    private final String description;
    private final List<ResourceFunction> resourceFunctions;
    private final List<RemoteFunction> remoteFunctions;

    public Service(String serviceName, Position position, String description, List<ResourceFunction> resourceFunctions, List<RemoteFunction> remoteFunctions) {
        this.serviceName = serviceName;
        this.position = position;
        this.description = description;
        this.resourceFunctions = resourceFunctions;
        this.remoteFunctions = remoteFunctions;
    }

    public List<ResourceFunction> getResourceFunctions() {
        return resourceFunctions;
    }

    public List<RemoteFunction> getRemoteFunctions() {
        return remoteFunctions;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Position getPosition() {
        return position;
    }
}
