package io.ballerina.graphqlmodelgenerator.core.model;

public class InteractionId {
    private final String serviceId;
    private final String path;

    public InteractionId(String serviceId, String path) {
        this.serviceId = serviceId;
        this.path = path;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getPath() {
        return path;
    }
}
