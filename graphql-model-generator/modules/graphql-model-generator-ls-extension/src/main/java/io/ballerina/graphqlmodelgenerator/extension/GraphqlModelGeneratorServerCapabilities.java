package io.ballerina.graphqlmodelgenerator.extension;

import org.ballerinalang.langserver.commons.registration.BallerinaServerCapability;

public class GraphqlModelGeneratorServerCapabilities extends BallerinaServerCapability {
    public GraphqlModelGeneratorServerCapabilities() {
        super(GraphqlModelGeneratorConstants.CAPABILITY_NAME);
    }
}
