package io.ballerina.sequencemodelgenerator.core.model;

public class DoStatement extends DiagramElementWithChildren{
    private OnFailStatement onFailStatement;


    public DoStatement(boolean isHidden) {
            super("DoStatement", isHidden);
        }

        public void setOnFailStatement(OnFailStatement onFailStatement) {
            this.onFailStatement = onFailStatement;
        }

    public OnFailStatement getOnFailStatement() {
        return onFailStatement;
    }
}
