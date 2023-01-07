package io.ballerina.graphqlmodelgenerator.extension;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

public class GraphqlDesignServiceRequest {
    private LineRange lineRange;

    public GraphqlDesignServiceRequest(LineRange lineRange) {
        this.lineRange = lineRange;
    }

    public LineRange getLineRange() {
        return lineRange;
    }

    public String getFilePath() {
        return this.lineRange.filePath();
    }

    public LinePosition getStartPosition() {
        return this.lineRange.startLine();
    }

    public LinePosition getEndPosition() {
        return this.lineRange.endLine();
    }
}
