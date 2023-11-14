package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class OnFailClause extends DElement {
    private String type;
    private String name;

        public OnFailClause(String type, String name, boolean isHidden, LineRange location) {
            super("OnFailStatement", isHidden, location);
            this.type = type;
            this.name = name;
        }
}
