package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class DNode {
    private final String kind;
    private final boolean isHidden;
    private final LineRange location;

    public DNode(String kind, boolean isHidden, LineRange location) {
        this.kind = kind;
        this.isHidden = isHidden;
        this.location = location;
    }

    public String getKind() {
        return kind;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public LineRange getLocation() {
        return location;
    }
}
