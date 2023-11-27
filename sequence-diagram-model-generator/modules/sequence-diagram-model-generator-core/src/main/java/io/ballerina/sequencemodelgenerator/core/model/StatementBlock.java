package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

/**
 * Represents the annotated comment statements of type //@sq-comment: text.
 * This special comment will be used to add comments to the sequence diagram,
 * which will explain a certain statement/statement block.
 *
 * @since 2201.8.0
 */
public class StatementBlock extends DElement {

    private String statementBlockText;

    public void setStatementBlockText(String statementBlockText) {
        this.statementBlockText = statementBlockText;
    }

    public String getStatementBlockText() {
        return statementBlockText;
    }

    public StatementBlock(LineRange location) {
        super("StatementBlock", false, location);
    }

}
