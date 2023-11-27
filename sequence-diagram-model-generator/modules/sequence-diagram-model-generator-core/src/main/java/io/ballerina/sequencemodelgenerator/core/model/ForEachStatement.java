package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the ForEach statement in the sequence diagram model.
 *
 * @since 2201.8.0
 */
public class ForEachStatement extends DElement {
    private final String condition;
    private OnFailClause onFailClause;

    public ForEachStatement(String condition, boolean isHidden, LineRange location) {
        super("ForEachStatement", isHidden, location);
        this.condition = condition;
    }

    public String getCondition() {
        return condition;
    }

    public OnFailClause getOnFailClause() {
        return onFailClause;
    }

    public void setOnFailClause(OnFailClause onFailClause) {
        this.onFailClause = onFailClause;
    }

}
