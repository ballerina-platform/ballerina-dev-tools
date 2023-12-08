package io.ballerina.workermodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.Arrays;
import java.util.List;

/**
 * Generator for the worker model.
 *
 * @since 2201.9.0
 */
public class ModelGenerator {

    private final SemanticModel semanticModel;
    private final Document document;
    private final LineRange lineRange;
    private static final List<SyntaxKind> validCanvasNodeKinds = Arrays.asList(
            SyntaxKind.FUNCTION_DEFINITION,
            SyntaxKind.RESOURCE_ACCESSOR_DEFINITION
    );

    public ModelGenerator(SemanticModel model, Document document, LineRange lineRange) {
        this.semanticModel = model;
        this.document = document;
        this.lineRange = lineRange;
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

        // Check if the canvas node is a valid type
        if (!validCanvasNodeKinds.contains(canvasNode.kind())) {
            throw new Exception();
        }

        // Build the flow diagram
        FlowBuilder flowBuilder = new FlowBuilder("1", "flow1", "path", semanticModel);
        canvasNode.accept(flowBuilder);

        // Convert the flow diagram to a JSON object
        Gson gson = new Gson();
        return gson.toJsonTree(flowBuilder.build());
    }
}
