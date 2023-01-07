package io.ballerina.graphqlmodelgenerator.core.model;

import java.util.List;

public class Record {
    private final String recordIdentifier;
    private List<Field> attributes;

    public Record(String recordIdentifier, List<Field> attributes) {
        this.recordIdentifier = recordIdentifier;
        this.attributes = attributes;
    }
}
