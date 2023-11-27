package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the Else statement in the sequence diagram model.
 *
 * @since 2201.8.0
 */
public class ElseStatement extends DElement {

    public ElseStatement(boolean isHidden, LineRange location) {
        super("ElseStatement", isHidden, location);
    }
}
