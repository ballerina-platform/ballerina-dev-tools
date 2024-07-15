package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeAttributes;
import io.ballerina.flowmodelgenerator.core.model.node.NodeId;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates the node template for the given node kind.
 *
 * @since 1.4.0
 */
public class NodeTemplateGenerator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<FlowNode.Kind, FlowNode> nodeCache = new HashMap<>();

    public JsonElement getNodeTemplate(JsonObject id) {
        NodeId nodeId = gson.fromJson(id, NodeId.class);
        FlowNode.Kind kind = FlowNode.Kind.valueOf(nodeId.kind());
        FlowNode flowNode = nodeCache.get(kind);
        if (flowNode != null) {
            return gson.toJsonTree(flowNode);
        }

        flowNode = FlowNode.getNodeFromKind(kind);
        flowNode.setConstData();
        flowNode.setTemplateData();
        nodeCache.put(flowNode.kind(), flowNode);
        return gson.toJsonTree(flowNode);
    }
}
