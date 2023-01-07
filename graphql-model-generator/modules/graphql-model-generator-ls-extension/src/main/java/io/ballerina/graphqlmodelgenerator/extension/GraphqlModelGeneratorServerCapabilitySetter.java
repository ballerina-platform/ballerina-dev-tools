package io.ballerina.graphqlmodelgenerator.extension;

import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter;

import java.util.Optional;

@JavaSPIService("org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter")
public class GraphqlModelGeneratorServerCapabilitySetter  extends
        BallerinaServerCapabilitySetter<GraphqlModelGeneratorServerCapabilities> {

    @Override
    public Optional<GraphqlModelGeneratorServerCapabilities> build() {

        GraphqlModelGeneratorServerCapabilities capabilities = new GraphqlModelGeneratorServerCapabilities();
        return Optional.of(capabilities);
    }

    @Override
    public String getCapabilityName() {
        return GraphqlModelGeneratorConstants.CAPABILITY_NAME;
    }

    @Override
    public Class<GraphqlModelGeneratorServerCapabilities> getCapability() {
        return GraphqlModelGeneratorServerCapabilities.class;
    }
}
