package io.ballerina.graphqlmodelgenerator.extension;


import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.registration.BallerinaClientCapabilitySetter;

@JavaSPIService("org.ballerinalang.langserver.commons.registration.BallerinaClientCapabilitySetter")
public class GraphqlModelGeneratorClientCapabilitySetter extends
        BallerinaClientCapabilitySetter<GraphqlModelGeneratorClientCapabilities> {
    @Override
    public String getCapabilityName() {
        return GraphqlModelGeneratorConstants.CAPABILITY_NAME;
    }

    @Override
    public Class<GraphqlModelGeneratorClientCapabilities> getCapability() {
        return GraphqlModelGeneratorClientCapabilities.class;
    }
}
