package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class EnumComponent {
    private final String name;
    private final List<EnumField> enumFields;

    public EnumComponent(String name, List<EnumField> enumFields) {
        this.name = name;
        this.enumFields = enumFields;
    }
}
