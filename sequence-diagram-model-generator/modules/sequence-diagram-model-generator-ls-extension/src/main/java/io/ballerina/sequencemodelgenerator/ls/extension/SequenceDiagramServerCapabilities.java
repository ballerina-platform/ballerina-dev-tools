package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.langserver.commons.registration.BallerinaServerCapability;

/**
 * Server capabilities for the sequence diagram model generator service.
 *
 * @since 2201.8.0
 */
public class SequenceDiagramServerCapabilities extends BallerinaServerCapability {
    public SequenceDiagramServerCapabilities() {
        super(SequenceDiagramConstants.CAPABILITY_NAME);
    }
}
