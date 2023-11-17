package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;

public class DElementBody extends DNode {
    private List<DNode> childElements;

    public DElementBody(String kind, boolean isHidden, LineRange location) {
        super(kind, isHidden, location);
        this.childElements = new ArrayList<>();
    }

    public void addChildDiagramElement(DNode DNode) {
        childElements.add(DNode);
    }

    public List<DNode> getChildElements() {
        return childElements;
    }
}
