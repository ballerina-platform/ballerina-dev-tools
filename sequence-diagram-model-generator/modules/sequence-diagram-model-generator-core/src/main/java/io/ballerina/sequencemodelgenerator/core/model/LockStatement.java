package io.ballerina.sequencemodelgenerator.core.model;

public class LockStatement extends DiagramElementWithChildren{
    private OnFailStatement onFailStatement;

    public LockStatement(boolean isHidden) {
        super("LockStatement", isHidden);
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }
}
