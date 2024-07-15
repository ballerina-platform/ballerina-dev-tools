package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.ExpressionAttributes;
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

    private static final Map<NodeId, FlowNode> nodeCache = new HashMap<>();

    public JsonElement getNodeTemplate(JsonObject id) {
        NodeId nodeId = gson.fromJson(id, NodeId.class);
        FlowNode flowNode = nodeCache.get(nodeId);
        if (flowNode != null) {
            return gson.toJsonTree(flowNode);
        }

        flowNode = FlowNode.getNodeFromKind(FlowNode.Kind.valueOf(nodeId.kind()));
        flowNode.setConstData();
        flowNode.setTemplateData();
        if (nodeId.library() != null) {
            NodeAttributes.Info info = NodeAttributes.get(nodeId.library() + "-" + nodeId.call());
            flowNode.label = info.label();

            Map<String, Expression> nodeProperties = new HashMap<>();
            ExpressionAttributes.Info callExpressionInfo = info.callExpression();
            nodeProperties.put(callExpressionInfo.key(), Expression.getExpressionForInfo(callExpressionInfo));
            info.parameterExpressions().forEach(expressionInfo -> {
                Expression expression = Expression.getExpressionForInfo(expressionInfo);
                nodeProperties.put(expressionInfo.key(), expression);
            });
            flowNode.nodeProperties = nodeProperties;
        }

        nodeCache.put(nodeId, flowNode);
        return gson.toJsonTree(flowNode);
    }
}
