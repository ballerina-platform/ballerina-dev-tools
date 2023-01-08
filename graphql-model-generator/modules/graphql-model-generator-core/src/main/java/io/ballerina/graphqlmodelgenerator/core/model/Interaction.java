package io.ballerina.graphqlmodelgenerator.core.model;

public class Interaction {
    private final String componentName;

    public Interaction(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }
}
