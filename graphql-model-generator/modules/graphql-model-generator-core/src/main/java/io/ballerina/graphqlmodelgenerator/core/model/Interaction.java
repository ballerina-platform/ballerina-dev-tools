package io.ballerina.graphqlmodelgenerator.core.model;

public class Interaction {
    private final String componentName;
    private final String path;

    public Interaction(String componentName, String path) {
        this.componentName = componentName;
        this.path = path;
    }

    public String getComponentName() {
        return componentName;
    }
}
