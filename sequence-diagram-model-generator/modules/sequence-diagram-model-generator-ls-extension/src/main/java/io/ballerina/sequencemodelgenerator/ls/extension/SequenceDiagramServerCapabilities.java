package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.langserver.commons.registration.BallerinaServerCapability;

public class SequenceDiagramServerCapabilities extends BallerinaServerCapability {
    public SequenceDiagramServerCapabilities() {
        super(SequenceDiagramConstants.CAPABILITY_NAME);
    }
}
