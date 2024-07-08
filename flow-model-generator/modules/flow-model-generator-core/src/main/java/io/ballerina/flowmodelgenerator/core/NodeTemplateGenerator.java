package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.While;

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
            case IF -> If.DEFAULT_NODE;
            case HTTP_API_GET_CALL -> null;
            case HTTP_API_POST_CALL -> null;
            case RETURN -> Return.DEFAULT_NODE;
            case EXPRESSION -> DefaultExpression.DEFAULT_NODE;
            case ERROR_HANDLER -> ErrorHandler.DEFAULT_NODE;
            case WHILE -> While.DEFAULT_NODE;
            case CONTINUE -> Continue.DEFAULT_NODE;
            case BREAK -> Break.DEFAULT_NODE;
            case START -> Start.DEFAULT_NODE;
        };
        return gson.toJsonTree(flowNode);
    }
}
