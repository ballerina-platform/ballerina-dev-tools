package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.JsonElement;

public class GraphqlDesignServiceResponse {
    private JsonElement graphqlDesignModel;

    public JsonElement getGraphqlDesignModel() {
        return graphqlDesignModel;
    }

    public void setGraphqlDesignModel(JsonElement graphqlDesignModel) {
        this.graphqlDesignModel = graphqlDesignModel;
    }
}
