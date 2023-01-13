package io.ballerina.graphqlmodelgenerator.extension;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

public class GraphqlDesignServiceRequest {
    private String filePath;
    private LinePosition startLine;
    private LinePosition endLine;

    public GraphqlDesignServiceRequest(String filePath, LinePosition startLine, LinePosition endLine) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getFilePath() {
        return filePath;
    }

    public LineRange getLineRange(){
        LineRange lineRange = LineRange.from(filePath,startLine,endLine);
        return lineRange;
    }
}
