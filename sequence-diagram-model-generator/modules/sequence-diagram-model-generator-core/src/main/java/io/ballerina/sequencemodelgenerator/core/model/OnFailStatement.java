package io.ballerina.sequencemodelgenerator.core.model;

public class OnFailStatement extends DiagramElementWithChildren{
    private String type;
    private String name;

        public OnFailStatement(String type, String name, boolean isHidden) {
            super("OnFailStatement", isHidden);
            this.type = type;
            this.name = name;
        }
}
