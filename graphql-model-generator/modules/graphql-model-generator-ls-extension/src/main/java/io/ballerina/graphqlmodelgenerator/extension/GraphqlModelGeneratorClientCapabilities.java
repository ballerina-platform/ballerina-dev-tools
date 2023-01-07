package io.ballerina.graphqlmodelgenerator.extension;

import org.ballerinalang.langserver.commons.registration.BallerinaClientCapability;

public class GraphqlModelGeneratorClientCapabilities extends BallerinaClientCapability {
    public GraphqlModelGeneratorClientCapabilities() {
        super(GraphqlModelGeneratorConstants.CAPABILITY_NAME);
    }
}
