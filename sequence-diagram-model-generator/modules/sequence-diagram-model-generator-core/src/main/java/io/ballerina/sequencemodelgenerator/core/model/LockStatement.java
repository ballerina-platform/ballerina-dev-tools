package io.ballerina.sequencemodelgenerator.core.model;

public class LockStatement extends DiagramElementWithChildren{
    private OnFailStatement onFailStatement;

    public LockStatement() {
        super("LockStatement");
    }

    public void setOnFailStatement(OnFailStatement onFailStatement) {
        this.onFailStatement = onFailStatement;
    }


}
