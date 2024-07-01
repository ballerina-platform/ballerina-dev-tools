package io.ballerina.flowmodelgenerator.extension.response;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents the response for the flow model getNodeTemplate API.
 *
 * @since 1.4.0
 */
public class FlowModelNodeTemplateResponse {

    private final JsonElement flowNode;

    public FlowModelNodeTemplateResponse(JsonElement flowNode) {
        this.flowNode = flowNode;
    }

    public JsonElement flowNode() {
        return flowNode;
    }
}
