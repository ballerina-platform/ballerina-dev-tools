package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

public class ElseStatement extends DiagramElementWithChildren{

    public ElseStatement(boolean isHidden, LineRange location) {
        super("ElseStatement", isHidden, location);
    }
}
