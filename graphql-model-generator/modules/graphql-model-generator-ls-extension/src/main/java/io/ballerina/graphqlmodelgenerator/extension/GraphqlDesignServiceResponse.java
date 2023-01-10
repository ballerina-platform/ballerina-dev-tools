package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.JsonElement;
import io.ballerina.graphqlmodelgenerator.core.diagnostic.GraphqlModelDiagnostic;

public class GraphqlDesignServiceResponse {
    private JsonElement graphqlDesignModel;
    private GraphqlModelDiagnostic diagnostic;

    public GraphqlModelDiagnostic getDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(GraphqlModelDiagnostic diagnostic) {
        this.diagnostic = diagnostic;
    }

    public JsonElement getGraphqlDesignModel() {
        return graphqlDesignModel;
    }

    public void setGraphqlDesignModel(JsonElement graphqlDesignModel) {
        this.graphqlDesignModel = graphqlDesignModel;
    }
}
