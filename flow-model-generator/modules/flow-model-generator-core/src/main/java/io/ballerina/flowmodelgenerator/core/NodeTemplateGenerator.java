package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
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

    public JsonElement getNodeTemplate(WorkspaceManager workspaceManager, Path filePath, LinePosition position,
                                       JsonObject id) {
        Codedata codedata = gson.fromJson(id, Codedata.class);
        FlowNode flowNode = nodeCache.get(codedata);
        if (flowNode != null) {
            return gson.toJsonTree(flowNode);
        }

        flowNode = NodeBuilder.getNodeFromKind(codedata.node())
                .setConstData()
                .setTemplateData(new NodeBuilder.TemplateContext(workspaceManager, filePath, position, codedata))
                .build();

        // TODO: Need to keep an array on which nodes are note not cacheable
        if (codedata.node() != NodeKind.DATA_MAPPER) {
            nodeCache.put(codedata, flowNode);
        }
        return gson.toJsonTree(flowNode);
    }
}
