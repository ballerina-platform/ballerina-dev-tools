package io.ballerina.workermodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import io.ballerina.workermodelgenerator.core.model.Endpoint;

import java.nio.file.Path;
import java.util.Map;

/**
 * Generator for the worker model.
 *
 * @since 2201.9.0
 */
public class ModelGenerator {

    private final SemanticModel semanticModel;
    private final Document document;
    private final LineRange lineRange;
    private final Path filePath;
    private final Gson gson;

    public ModelGenerator(SemanticModel model, Document document, LineRange lineRange, Path filePath) {
        this.semanticModel = model;
        this.document = document;
        this.lineRange = lineRange;
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    /**
     * Generates a worker model for the given canvas node.
     *
     * @return JSON representation of the worker model
     * @throws Exception if the canvas is not valid
     */
    public JsonElement getWorkerModel() throws Exception {
        // Obtain the code block representing the canvas
        SyntaxTree syntaxTree = document.syntaxTree();
        TextDocument textDocument = syntaxTree.textDocument();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        NonTerminalNode canvasNode = modulePartNode.findNode(TextRange.from(start, end - start), true);

        // Analyze the symbols in the document
        DocumentSymbolFinder documentSymbolFinder = new DocumentSymbolFinder();
        modulePartNode.accept(documentSymbolFinder);

        // Build the flow diagram
        Map<String, String> endpointMap = documentSymbolFinder.getEndpointMap();
        FlowBuilder flowBuilder = new FlowBuilder(semanticModel, modulePartNode, endpointMap);
        flowBuilder.setFilePath(this.filePath.toString());
        flowBuilder.setFileSourceRange(CommonUtils.getCodeLocationFromNode(modulePartNode));
        endpointMap.forEach((key, value) -> flowBuilder.addEndpoint(new Endpoint(key, value)));
        canvasNode.accept(flowBuilder);
        return gson.toJsonTree(flowBuilder.build());
    }
}
