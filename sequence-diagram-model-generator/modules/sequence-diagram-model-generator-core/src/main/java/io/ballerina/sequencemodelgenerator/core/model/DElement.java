package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

public class DElement extends DNode {
    private DElementBody elementBody;
    public DElement(String kind, boolean isHidden, LineRange location) {
        super(kind, isHidden, location);
    }

    public DElementBody getElementBody() {
        return elementBody;
    }

    public void addChildDiagramElements(DNode DNode) {
        if (this.elementBody == null) {
            this.elementBody = new DElementBody(this.getKind()+"Body", this.isHidden(), this.getLocation());
            this.elementBody.addChildDiagramElement(DNode);
        } else {
            this.elementBody.addChildDiagramElement(DNode);
        }
    }
}
