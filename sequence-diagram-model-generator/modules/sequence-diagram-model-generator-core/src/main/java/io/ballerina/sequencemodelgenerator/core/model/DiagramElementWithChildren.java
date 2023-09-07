package io.ballerina.sequencemodelgenerator.core.model;

public class DiagramElementWithChildren extends DiagramElement{
    private DiagramElementBody childElements;

    public DiagramElementWithChildren(String kind, boolean isHidden) {
        super(kind, isHidden);
    }

    public DiagramElementBody getChildElements() {
        return childElements;
    }

    public void addChildDiagramElements(DiagramElement diagramElement) {
        if (this.childElements == null) {
            this.childElements = new DiagramElementBody(this.getKind()+"Body", this.isHidden());
            this.childElements.addChildDiagramElement(diagramElement);
        } else {
            this.childElements.addChildDiagramElement(diagramElement);
        }
    }

}
