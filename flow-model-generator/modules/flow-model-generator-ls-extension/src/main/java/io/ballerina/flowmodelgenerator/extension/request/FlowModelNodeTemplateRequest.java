package io.ballerina.flowmodelgenerator.extension.request;

import com.google.gson.JsonObject;

/**
 * Represents a request to the flow model getNodeTemplate API.
 *
 * @param id id of the available node
 * @since 1.4.0
 */
public record FlowModelNodeTemplateRequest(JsonObject id) {

}
