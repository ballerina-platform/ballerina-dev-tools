package io.ballerina.sequencemodelgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

public class DiagramElementWithChildren extends DiagramElement{
    private List<DiagramElement> childElements;

    public DiagramElementWithChildren(String kind) {
        super(kind);
    }

    public List<DiagramElement> getChildElements() {
        return childElements;
    }

    public void addChildDiagramElements(DiagramElement diagramElement) {
        if (this.childElements == null) {
            this.childElements = new ArrayList<>();
            this.childElements.add(diagramElement);
        } else {
            this.childElements.add(diagramElement);
        }
    }

    public void setChildElements(List<DiagramElement> childElements) {
        this.childElements = childElements;
    }
}
