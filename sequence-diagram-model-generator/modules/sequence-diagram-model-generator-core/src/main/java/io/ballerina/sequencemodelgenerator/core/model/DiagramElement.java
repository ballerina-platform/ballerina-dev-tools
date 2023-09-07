package io.ballerina.sequencemodelgenerator.core.model;

public class DiagramElement {
    private final String kind;
    private boolean isHidden;

    public DiagramElement(String kind, boolean isHidden) {
        this.kind = kind;
        this.isHidden = isHidden;
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
}
