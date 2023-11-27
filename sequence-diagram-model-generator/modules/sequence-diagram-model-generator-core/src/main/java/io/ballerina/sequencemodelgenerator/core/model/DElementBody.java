package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the body of an element {@link DElement} in the sequence diagram model.
 *
 * @since 2201.8.0
 */
public class DElementBody extends DNode {
    private final List<DNode> childElements;

    public DElementBody(String kind, boolean isHidden, LineRange location) {
        super(kind, isHidden, location);
        this.childElements = new ArrayList<>();
    }

    public void addChildDiagramElement(DNode dNode) {
        childElements.add(dNode);
    }

    public List<DNode> getChildElements() {
        return childElements;
    }
}
