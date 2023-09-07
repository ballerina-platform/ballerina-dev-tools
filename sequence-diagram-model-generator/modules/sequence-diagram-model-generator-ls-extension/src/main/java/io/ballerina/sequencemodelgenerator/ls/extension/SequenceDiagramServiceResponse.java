package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.JsonElement;

public class SequenceDiagramServiceResponse {
    private JsonElement sequenceDiagramModel;
    private ModelDiagnostic modelDiagnostic;

    public JsonElement getSequenceDiagramModel() {
        return sequenceDiagramModel;
    }

    public void setSequenceDiagramModel(JsonElement sequenceDiagramModel) {
        this.sequenceDiagramModel = sequenceDiagramModel;
    }

    public void setModelDiagnostic(ModelDiagnostic modelDiagnostic) {
        this.modelDiagnostic = modelDiagnostic;
    }
}
