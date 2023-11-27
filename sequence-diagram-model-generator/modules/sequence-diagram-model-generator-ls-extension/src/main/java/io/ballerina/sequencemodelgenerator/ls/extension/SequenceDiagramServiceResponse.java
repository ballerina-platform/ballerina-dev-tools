package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.JsonElement;

/**
 * Represents the response from the sequence diagram model generator service.
 *
 * @since 2201.8.0
 */
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
