package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class Service {
    private final String serviceType;
    private final String serviceName;
    private final List<ResourceFunction> resourceFunctions;
    private final List<RemoteFunction> remoteFunctions;

    public Service(String serviceType, String serviceName, List<ResourceFunction> resourceFunctions, List<RemoteFunction> remoteFunctions) {
        this.serviceType = serviceType;
        this.serviceName = serviceName;
        this.resourceFunctions = resourceFunctions;
        this.remoteFunctions = remoteFunctions;
    }

    public List<ResourceFunction> getResourceFunctions() {
        return resourceFunctions;
    }

    public List<RemoteFunction> getRemoteFunctions() {
        return remoteFunctions;
    }
}
