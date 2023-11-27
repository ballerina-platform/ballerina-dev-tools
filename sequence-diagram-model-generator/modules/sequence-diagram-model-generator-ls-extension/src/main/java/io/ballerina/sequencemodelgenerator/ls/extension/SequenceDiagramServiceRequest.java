package io.ballerina.sequencemodelgenerator.ls.extension;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

/**
 * Represents the sequence diagram service request.
 *
 * @since 2201.8.0
 */
public class SequenceDiagramServiceRequest {
    private final String filePath;
    private final LinePosition startLine;
    private final LinePosition endLine;

    public SequenceDiagramServiceRequest(String filePath, LinePosition startLine, LinePosition endLine) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getFilePath() {
        return filePath;
    }

    public LineRange getLineRange() {
        LineRange lineRange = LineRange.from(filePath, startLine, endLine);
        return lineRange;
    }
}
