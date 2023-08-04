package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.langserver.commons.registration.BallerinaClientCapability;

public class SequenceDiagramClientCapabilities extends BallerinaClientCapability {

    public SequenceDiagramClientCapabilities() {
        super(SequenceDiagramConstants.CAPABILITY_NAME);
    }
}
