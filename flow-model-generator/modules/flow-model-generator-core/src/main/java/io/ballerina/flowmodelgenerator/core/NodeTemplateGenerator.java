package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.node.BreakNode;
import io.ballerina.flowmodelgenerator.core.model.node.ContinueNode;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandlerNode;
import io.ballerina.flowmodelgenerator.core.model.node.IfNode;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.StartNode;
import io.ballerina.flowmodelgenerator.core.model.node.WhileNode;

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
            case IF -> IfNode.DEFAULT_NODE;
            case HTTP_API_GET_CALL -> null;
            case HTTP_API_POST_CALL -> null;
            case RETURN -> Return.DEFAULT_NODE;
            case EXPRESSION -> DefaultExpression.DEFAULT_NODE;
            case ERROR_HANDLER -> ErrorHandlerNode.DEFAULT_NODE;
            case WHILE -> WhileNode.DEFAULT_NODE;
            case CONTINUE -> ContinueNode.DEFAULT_NODE;
            case BREAK -> BreakNode.DEFAULT_NODE;
            case START -> StartNode.DEFAULT_NODE;
        };
        return gson.toJsonTree(flowNode);
    }
}
