package io.ballerina.flowmodelgenerator.extension.request;

import io.ballerina.tools.text.LineRange;

public class FlowModelAvailableNodesRequest {

    private LineRange parentNodeLineRange;
    private String parentNodeKind;
    String branchLabel;

    public FlowModelAvailableNodesRequest(LineRange parentNodeLineRange, String parentNodeKind,
                                          String branchLabel) {
        this.parentNodeLineRange = parentNodeLineRange;
        this.parentNodeKind = parentNodeKind;
        this.branchLabel = branchLabel;
    }

    public LineRange parentNodeLineRange() {
        return parentNodeLineRange;
    }

    public String parentNodeKind() {
        return parentNodeKind;
    }

    public String branchLabel() {
        return branchLabel;
    }
}
