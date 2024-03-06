package io.ballerina.flowmodelgenerator.extension.response;

import com.google.gson.JsonArray;

public class FlowModelAvailableNodesResponse {

    private JsonArray availableNodes;

    public void setAvailableNodes(JsonArray availableNodes) {
        this.availableNodes = availableNodes;
    }

    public JsonArray availableNodes() {
        return availableNodes;
    }
}
