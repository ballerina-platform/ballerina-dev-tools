package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.registration.BallerinaClientCapabilitySetter;

/**
 * Client capability setter for the sequence diagram model generator service.
 *
 * @since 2201.8.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.registration.BallerinaClientCapabilitySetter")
public class SequenceDiagramClientCapabilitySetter extends
        BallerinaClientCapabilitySetter<SequenceDiagramClientCapabilities> {

    @Override
    public String getCapabilityName() {
        return SequenceDiagramConstants.CAPABILITY_NAME;
    }

    @Override
    public Class<SequenceDiagramClientCapabilities> getCapability() {
        return SequenceDiagramClientCapabilities.class;
    }
}
