package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;

/**
 * Generates the node template for the given node kind.
 *
 * @since 2.0.0
 */
public class NodeTemplateGenerator {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final LSClientLogger lsClientLogger;

    public NodeTemplateGenerator(LSClientLogger lsClientLogger) {
        this.lsClientLogger = lsClientLogger;
    }

    public JsonElement getNodeTemplate(WorkspaceManager workspaceManager, Path filePath, LinePosition position,
                                       JsonObject id) {
        Codedata codedata = gson.fromJson(id, Codedata.class);
        FlowNode flowNode = NodeBuilder.getNodeFromKind(codedata.node())
                .setConstData()
                .setTemplateData(new NodeBuilder.TemplateContext(workspaceManager, filePath, position, codedata, lsClientLogger))
                .build();
        return gson.toJsonTree(flowNode);
    }
}
