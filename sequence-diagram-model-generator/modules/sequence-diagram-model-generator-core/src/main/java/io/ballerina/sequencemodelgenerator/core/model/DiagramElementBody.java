package io.ballerina.sequencemodelgenerator.core.model;

import java.util.ArrayList;
import java.util.List;

public class DiagramElementBody extends DiagramElement {
    private List<DiagramElement> childElementList;

    public DiagramElementBody(String kind, boolean isHidden) {
        super(kind, isHidden);
        this.childElementList = new ArrayList<>();
    }

    public void addChildDiagramElement(DiagramElement diagramElement) {
        childElementList.add(diagramElement);
    }

}
