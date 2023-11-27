package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the Lock statement.
 *
 * @since 2201.8.0
 */
public class LockStatement extends DElement {
    private OnFailClause onFailClause;

    public LockStatement(boolean isHidden, LineRange location) {
        super("LockStatement", isHidden, location);
    }

    public void setOnFailClause(OnFailClause onFailClause) {
        this.onFailClause = onFailClause;
    }

    public OnFailClause getOnFailClause() {
        return onFailClause;
    }
}
