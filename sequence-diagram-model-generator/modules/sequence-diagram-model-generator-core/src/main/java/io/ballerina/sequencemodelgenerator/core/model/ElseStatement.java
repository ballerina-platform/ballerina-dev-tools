package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class ElseStatement extends StatementWithBody{

    public ElseStatement( List<Statement> statements) {
        super("ELSE", statements);
    }
}
