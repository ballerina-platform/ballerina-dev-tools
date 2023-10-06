package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class DiagramElementWithChildren extends DiagramElement{
    private DiagramElementBody childElements;

    public DiagramElementWithChildren(String kind, boolean isHidden, LineRange location) {
        super(kind, isHidden, location);
    }

    public DiagramElementBody getChildElements() {
        return childElements;
    }

    public void addChildDiagramElements(DiagramElement diagramElement) {
        if (this.childElements == null) {
            this.childElements = new DiagramElementBody(this.getKind()+"Body", this.isHidden(), this.getLocation());
            this.childElements.addChildDiagramElement(diagramElement);
        } else {
            this.childElements.addChildDiagramElement(diagramElement);
        }
    }

}
