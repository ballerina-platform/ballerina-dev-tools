package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.node.BreakNode;
import io.ballerina.flowmodelgenerator.core.model.node.ContinueNode;

/**
 * Generates the node template for the given node kind.
 *
 * @since 1.4.0
 */
public class NodeTemplateGenerator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public JsonElement getNodeTemplate(String kind) {
        FlowNode flowNode = switch (FlowNode.Kind.valueOf(kind)) {
            case EVENT_HTTP_API -> null;
            case IF -> null;
            case HTTP_API_GET_CALL -> null;
            case HTTP_API_POST_CALL -> null;
            case RETURN -> null;
            case EXPRESSION -> null;
            case ERROR_HANDLER -> null;
            case WHILE -> null;
            case CONTINUE -> ContinueNode.DEFAULT_NODE;
            case BREAK -> BreakNode.DEFAULT_NODE;
        };
        return gson.toJsonTree(flowNode);
    }
}
