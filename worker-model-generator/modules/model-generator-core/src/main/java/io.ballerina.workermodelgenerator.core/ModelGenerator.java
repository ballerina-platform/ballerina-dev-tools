package io.ballerina.workermodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;

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

    public ModelGenerator(SemanticModel model, Document document, LineRange lineRange, Path filePath) {
        this.semanticModel = model;
        this.document = document;
        this.lineRange = lineRange;
        this.filePath = filePath;
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

        // Build the flow diagram
        FlowBuilder flowBuilder = new FlowBuilder(semanticModel);
        flowBuilder.setFilePath(this.filePath.toString());
        canvasNode.accept(flowBuilder);

        // Convert the flow diagram to a JSON object
        Gson gson = new Gson();
        return gson.toJsonTree(flowBuilder.build());
    }
}
