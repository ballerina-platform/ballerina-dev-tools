package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;
import io.ballerina.sequencemodelgenerator.core.model.Constants.InteractionType;

public class ReturnAction extends Interaction {
    private final String name;
    private final String type;

    public ReturnAction(String sourceId, String targetId, String name, String type, boolean isHidden, LineRange location) {
        super(sourceId, targetId, InteractionType.RETURN_ACTION, isHidden, location);
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
