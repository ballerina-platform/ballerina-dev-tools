package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the while statement.
 *
 * @since 2201.8.0
 */
public class WhileStatement extends DElement {
    private final String condition;
    private OnFailClause onFailClause;

    public WhileStatement(String condition, boolean isHidden, LineRange location) {
        super("WhileStatement", isHidden, location);
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
