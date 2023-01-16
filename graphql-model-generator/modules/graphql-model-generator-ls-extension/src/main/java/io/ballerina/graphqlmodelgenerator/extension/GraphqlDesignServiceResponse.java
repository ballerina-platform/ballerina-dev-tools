package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.JsonElement;

public class GraphqlDesignServiceResponse {
    private JsonElement graphqlDesignModel;
    private boolean isIncompleteModel;
    private String errorMsg;

    public boolean isIncompleteModel() {
        return isIncompleteModel;
    }

    public void setIncompleteModel(boolean incompleteModel) {
        isIncompleteModel = incompleteModel;
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
