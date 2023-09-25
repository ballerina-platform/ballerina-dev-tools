package io.ballerina.sequencemodelgenerator.core.model;

public class LockStatement extends DiagramElementWithChildren{
    private OnFailStatement onFailStatement;
    private boolean hasInteractions;

    public LockStatement(boolean isHidden) {
        super("LockStatement", isHidden);
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }

    public void setHasInteractions(boolean hasInteractions) {
        this.hasInteractions = hasInteractions;
    }
}
