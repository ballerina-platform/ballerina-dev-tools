package io.ballerina.flowmodelgenerator.extension.response;

import com.google.gson.JsonElement;

/**
 * Represents the response for the flow model getNodeTemplate API.
 *
 * @since 1.4.0
 */
public class FlowModelNodeTemplateResponse extends AbstractFlowModelResponse {

    private JsonElement flowNode;

    public void setFlowNode(JsonElement flowNode) {
        this.flowNode = flowNode;
    }

    public JsonElement flowNode() {
        return flowNode;
    }
}
