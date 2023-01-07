package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class Enum {
    private final String name;
    private final List<EnumField> enumFields;

    public Enum(String name, List<EnumField> enumFields) {
        this.name = name;
        this.enumFields = enumFields;
    }
}
