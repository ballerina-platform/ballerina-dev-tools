package io.ballerina.sequencemodelgenerator.core.model;

public class DoStatement extends DiagramElementWithChildren {
    private OnFailStatement onFailStatement;
    private boolean hasInteractions;


    public DoStatement(boolean isHidden) {
        super("DoStatement", isHidden);
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
