package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class LockStatement extends DElement {
    private OnFailStatement onFailStatement;

    public LockStatement(boolean isHidden, LineRange location) {
        super("LockStatement", isHidden, location);
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }
}
