package io.ballerina.sequencemodelgenerator.core.model;

public class DoStatement extends DiagramElementWithChildren{
    private OnFailStatement onFailStatement;


    public DoStatement() {
            super("DoStatement");
        }

        public void setOnFailStatement(OnFailStatement onFailStatement) {
            this.onFailStatement = onFailStatement;
        }
}
