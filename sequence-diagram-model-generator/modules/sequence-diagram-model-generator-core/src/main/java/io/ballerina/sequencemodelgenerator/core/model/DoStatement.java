package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the Do statement in the sequence diagram model.
 *
 * @since 2201.8.0
 */
public class DoStatement extends DElement {
    private OnFailClause onFailClause;

    public DoStatement(boolean isHidden, LineRange location) {
        super("DoStatement", isHidden, location);
    }

    public void setOnFailClause(OnFailClause onFailClause) {
        this.onFailClause = onFailClause;
    }

    public OnFailClause getOnFailClause() {
        return onFailClause;
    }
}
