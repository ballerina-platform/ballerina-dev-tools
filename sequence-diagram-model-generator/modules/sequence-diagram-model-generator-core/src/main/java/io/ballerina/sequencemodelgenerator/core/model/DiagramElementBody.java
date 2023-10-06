package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;

public class DiagramElementBody extends DiagramElement {
    private List<DiagramElement> childElementList;

    public DiagramElementBody(String kind, boolean isHidden, LineRange location) {
        super(kind, isHidden, location);
        this.childElementList = new ArrayList<>();
    }

    public void addChildDiagramElement(DiagramElement diagramElement) {
        childElementList.add(diagramElement);
    }

}
