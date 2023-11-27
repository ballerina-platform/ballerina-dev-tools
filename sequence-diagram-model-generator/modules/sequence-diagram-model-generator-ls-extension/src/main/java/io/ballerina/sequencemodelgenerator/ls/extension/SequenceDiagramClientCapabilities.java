package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.langserver.commons.registration.BallerinaClientCapability;

/**
 * Client capabilities for the sequence diagram model generator service.
 *
 * @since 2201.8.0
 */
public class SequenceDiagramClientCapabilities extends BallerinaClientCapability {

    public SequenceDiagramClientCapabilities() {
        super(SequenceDiagramConstants.CAPABILITY_NAME);
    }
}
