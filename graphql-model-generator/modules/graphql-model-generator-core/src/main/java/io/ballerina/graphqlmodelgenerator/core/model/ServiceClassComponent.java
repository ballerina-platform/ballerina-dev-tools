package io.ballerina.graphqlmodelgenerator.core.model;

import io.ballerina.stdlib.graphql.commons.types.Position;

import java.util.List;

public class ServiceClassComponent {
    private final String serviceName;
    private final Position position;
    private final String description;
    private final List<ServiceClassField> functions;

    public ServiceClassComponent(String serviceName, Position position, String description, List<ServiceClassField> functions) {
        this.serviceName = serviceName;
        this.position = position;
        this.description = description;
        this.functions = functions;
    }
}
