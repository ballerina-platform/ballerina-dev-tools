package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.JsonElement;

public class SequenceDiagramServiceResponse {
    private JsonElement sequenceDiagramModel;

    public JsonElement getSequenceDiagramModel() {
        return sequenceDiagramModel;
    }

    public void setSequenceDiagramModel(JsonElement sequenceDiagramModel) {
        this.sequenceDiagramModel = sequenceDiagramModel;
    }
}
