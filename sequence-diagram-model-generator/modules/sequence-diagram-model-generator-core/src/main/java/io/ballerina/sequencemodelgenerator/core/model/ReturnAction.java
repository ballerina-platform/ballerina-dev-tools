package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class ReturnAction extends Interaction{
    private String name;
    private String type;

    public ReturnAction(String sourceId, String targetId, String name, String type, boolean isHidden, LineRange location) {
        super(sourceId, targetId, "ReturnAction", isHidden, location);
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
