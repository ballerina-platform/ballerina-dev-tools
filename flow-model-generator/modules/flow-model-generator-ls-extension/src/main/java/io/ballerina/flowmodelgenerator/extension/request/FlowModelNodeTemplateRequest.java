package io.ballerina.flowmodelgenerator.extension.request;

import com.google.gson.JsonObject;
import io.ballerina.tools.text.LinePosition;

/**
 * Represents a request to the flow model getNodeTemplate API.
 *
 * @param filePath file path of the source file
 * @param position position of the node to be added
 * @param id       codedata of the available node
 * @since 1.4.0
 */
public record FlowModelNodeTemplateRequest(String filePath, LinePosition position, JsonObject id) {

}
