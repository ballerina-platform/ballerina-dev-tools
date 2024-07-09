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
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.While;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Generates the node template for the given node kind.
 *
 * @since 1.4.0
 */
public class NodeTemplateGenerator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<FlowNode.Kind, FlowNode> nodeCache = new HashMap<>();

    private static final Map<FlowNode.Kind, Supplier<? extends FlowNode>> constructorMap = Map.of(
            FlowNode.Kind.IF, If::new,
            FlowNode.Kind.RETURN, Return::new,
            FlowNode.Kind.EXPRESSION, DefaultExpression::new,
            FlowNode.Kind.ERROR_HANDLER, ErrorHandler::new,
            FlowNode.Kind.WHILE, While::new,
            FlowNode.Kind.CONTINUE, Continue::new,
            FlowNode.Kind.BREAK, Break::new,
            FlowNode.Kind.PANIC, Panic::new
    );

    public JsonElement getNodeTemplate(String kindStr) {
        FlowNode.Kind kind = FlowNode.Kind.valueOf(kindStr);
        FlowNode flowNode = nodeCache.get(kind);
        if (flowNode != null) {
            return gson.toJsonTree(flowNode);
        }

        flowNode = constructorMap.getOrDefault(kind, DefaultExpression::new).get();
        flowNode.setTemplateData();
        nodeCache.put(flowNode.kind(), flowNode);
        return gson.toJsonTree(flowNode);
    }
}
