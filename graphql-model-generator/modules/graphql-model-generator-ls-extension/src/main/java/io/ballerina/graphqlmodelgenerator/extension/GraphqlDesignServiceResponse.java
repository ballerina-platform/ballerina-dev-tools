package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.JsonElement;

public class GraphqlDesignServiceResponse {
    private JsonElement graphqlDesignModel;
    private boolean isGeneratorCompleted;
    private String errorMsg;

    public boolean isGeneratorCompleted() {
        return isGeneratorCompleted;
    }

    public void setGeneratorCompleted(boolean generatorCompleted) {
        isGeneratorCompleted = generatorCompleted;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public JsonElement getGraphqlDesignModel() {
        return graphqlDesignModel;
    }

    public void setGraphqlDesignModel(JsonElement graphqlDesignModel) {
        this.graphqlDesignModel = graphqlDesignModel;
    }
}
