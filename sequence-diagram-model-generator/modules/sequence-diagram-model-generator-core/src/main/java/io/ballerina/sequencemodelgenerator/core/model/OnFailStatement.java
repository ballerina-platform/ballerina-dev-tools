package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class OnFailStatement extends DiagramElementWithChildren{
    private String type;
    private String name;

        public OnFailStatement(String type, String name, boolean isHidden, LineRange location) {
            super("OnFailStatement", isHidden, location);
            this.type = type;
            this.name = name;
        }
}
