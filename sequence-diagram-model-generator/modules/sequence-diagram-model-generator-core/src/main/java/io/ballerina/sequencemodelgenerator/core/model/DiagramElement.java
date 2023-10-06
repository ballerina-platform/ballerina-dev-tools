package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class DiagramElement {
    private final String kind;
    private boolean isHidden;
    private LineRange location;

    public DiagramElement(String kind, boolean isHidden, LineRange location) {
        this.kind = kind;
        this.isHidden = isHidden;
        this.location = location;
    }

    public DiagramElement(String kind) {
        this.kind = kind;
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
