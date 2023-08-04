package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.registration.BallerinaClientCapabilitySetter;

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
