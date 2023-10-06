package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class DoStatement extends DiagramElementWithChildren {
    private OnFailStatement onFailStatement;

    public DoStatement(boolean isHidden, LineRange location) {
        super("DoStatement", isHidden, location);
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }
}
