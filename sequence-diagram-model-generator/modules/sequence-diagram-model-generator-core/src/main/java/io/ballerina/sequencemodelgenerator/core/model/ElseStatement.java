package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class ElseStatement extends DElement {

    public ElseStatement(boolean isHidden, LineRange location) {
        super("ElseStatement", isHidden, location);
    }
}
