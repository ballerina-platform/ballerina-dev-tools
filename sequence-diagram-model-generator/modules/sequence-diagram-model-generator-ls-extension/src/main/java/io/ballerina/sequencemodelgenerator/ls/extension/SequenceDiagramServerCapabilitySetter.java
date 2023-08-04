package io.ballerina.sequencemodelgenerator.ls.extension;

import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter;

import java.util.Optional;

@JavaSPIService("org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter")
public class SequenceDiagramServerCapabilitySetter extends
        BallerinaServerCapabilitySetter<SequenceDiagramServerCapabilities> {

    @Override
    public Optional<SequenceDiagramServerCapabilities> build() {

        SequenceDiagramServerCapabilities capabilities = new SequenceDiagramServerCapabilities();
        return Optional.of(capabilities);
    }

    @Override
    public String getCapabilityName() {
        return SequenceDiagramConstants.CAPABILITY_NAME;
    }

    @Override
    public Class<SequenceDiagramServerCapabilities> getCapability() {
        return SequenceDiagramServerCapabilities.class;
    }
}
