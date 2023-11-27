package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;
/**
 * Represent an element in the sequence diagram model.
 *
 * @since 2201.8.0
 */
public class DElement extends DNode {
    private DElementBody elementBody;

    public DElement(String kind, boolean isHidden, LineRange location) {
        super(kind, isHidden, location);
    }

    public DElementBody getElementBody() {
        return elementBody;
    }

    public void addChildDiagramElements(DNode dNode) {
        if (this.elementBody == null) {
            this.elementBody = new DElementBody(this.getKind() + "Body", this.isHidden(), this.getLocation());
            this.elementBody.addChildDiagramElement(dNode);
        } else {
            this.elementBody.addChildDiagramElement(dNode);
        }
    }
}
