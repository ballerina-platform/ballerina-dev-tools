package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents onFailClause.
 *
 * @since 2201.8.0
 */
public class OnFailClause extends DElement {
    private final String type;
    private final String name;

    public OnFailClause(String type, String name, boolean isHidden, LineRange location) {
        super("OnFailStatement", isHidden, location);
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
