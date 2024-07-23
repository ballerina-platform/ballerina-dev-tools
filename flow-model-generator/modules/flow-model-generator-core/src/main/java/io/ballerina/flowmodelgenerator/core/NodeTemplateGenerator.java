package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Generates the node template for the given node kind.
 *
 * @since 1.4.0
 */
public class NodeTemplateGenerator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Map<Codedata, FlowNode> nodeCache = new HashMap<>();

    public JsonElement getNodeTemplate(JsonObject id) {
        Codedata codedata = gson.fromJson(id, Codedata.class);
        FlowNode flowNode = nodeCache.get(codedata);
        if (flowNode != null) {
            return gson.toJsonTree(flowNode);
        }

        flowNode = NodeBuilder.getNodeFromKind(codedata.node())
                .setTemplateData()
                .setConstData()
                .build();
        if (codedata.module() != null) {
//            NodeAttributes.Info info = NodeAttributes.getByKey(codedata.module(), codedata.symbol());
//            flowNode.metadata().label() = info.label();
//
//            Map<String, Property> nodeProperties = new HashMap<>();
//            ExpressionAttributes.Info callExpressionInfo = info.callExpression();
//            nodeProperties.put(callExpressionInfo.key(), Property.getExpressionForInfo(callExpressionInfo));
//            info.parameterExpressions().forEach(expressionInfo -> {
//                Property property = Property.getExpressionForInfo(expressionInfo);
//                nodeProperties.put(expressionInfo.key(), property);
//            });
//            flowNode.properties = nodeProperties;
        }

        nodeCache.put(codedata, flowNode);
        return gson.toJsonTree(flowNode);
    }
}
