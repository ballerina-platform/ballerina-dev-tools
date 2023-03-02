package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class HierarchicalResourceComponent {
    private final String name;
    private final List<ResourceFunction> hierarchicalResources;

    public HierarchicalResourceComponent(String name, List<ResourceFunction> hierarchicalResources) {
        this.name = name;
        this.hierarchicalResources = hierarchicalResources;
    }
}
