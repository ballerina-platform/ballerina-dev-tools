package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.ArrayList;
import java.util.List;

public class SuggestedModelGenerator {

    private final List<LineRange> errorLocations;
    private boolean foundError;
    private int errorIndex;
    private final Gson gson;
    private final LineRange newLineRange;

    public SuggestedModelGenerator(Document document, LineRange newLineRange) {
        this.foundError = false;
        this.errorIndex = 0;
        this.newLineRange = newLineRange;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.errorLocations = new ArrayList<>();
        for (Diagnostic diagnostic : document.syntaxTree().diagnostics()) {
            errorLocations.add(diagnostic.location().lineRange());
        }
//        DiagnosticResult diagnosticResult = newProject.currentPackage().getCompilation().diagnosticResult();
//        Collection<Diagnostic> errors = diagnosticResult.errors();
//        List<LineRange> errorLocations = errors.stream().map(error -> error.location().lineRange()).toList();
    }

    public void markSuggestedNodes(JsonArray newNodes, int startIndex) {
        for (int i = startIndex; i < newNodes.size(); i++) {
            JsonObject newNode = newNodes.get(i).getAsJsonObject();
            LineRange lineRange = getLineRange(newNode);

            // A new statement
            if (PositionUtil.isWithinLineRange(lineRange, newLineRange)) {
                if (newNode.has("branches")) {
                    JsonArray newBranches = newNode.getAsJsonArray("branches");
                    for (int j = 0; j < newBranches.size(); j++) {
                        JsonObject newBranch = newBranches.get(j).getAsJsonObject();
                        if (PositionUtil.isWithinLineRange(getLineRange(newBranch), newLineRange)) {
                            markSuggestedNodes(newBranch.getAsJsonArray("children"), 0);
                            continue;
                        }
                        newBranch.addProperty("suggested", true);
                    }
                } else {
                    i = handleSuggestedNode(newNodes, i, newNode);
                }
                continue;
            }

            // New statements are within a branch
            if (PositionUtil.isWithinLineRange(newLineRange, lineRange)) {
                if (newNode.has("branches")) {
                    JsonArray newBranches = newNode.getAsJsonArray("branches");
                    for (int j = 0; j < newBranches.size(); j++) {
                        JsonObject newBranch = newBranches.get(j).getAsJsonObject();
                        if (PositionUtil.isWithinLineRange(newLineRange, getLineRange(newBranch))) {
                            markSuggestedNodes(newBranch.getAsJsonArray("children"), 0);
                            continue;
                        }
                        newBranch.addProperty("suggested", false);
                    }
                } else {
                    i = handleSuggestedNode(newNodes, i, newNode);
                }
                continue;
            }
            newNode.addProperty("suggested", false);
        }
    }

    private int handleSuggestedNode(JsonArray newNodes, int newIndex, JsonObject newNode) {
        if (errorLocations.isEmpty() || !foundError && !isErrorInNode(newNode)) {
            newNode.addProperty("suggested", true);
            return newIndex;
        }
        newNodes.remove(newIndex);
        if (!foundError) {
            foundError = true;
            errorIndex++;
        }
        return newIndex - 1;
    }

    private boolean isErrorInNode(JsonObject newNode) {
        LineRange lineRange =
                gson.fromJson(newNode.get("codedata").getAsJsonObject().get("lineRange"), LineRange.class);
        return PositionUtil.isWithinLineRange(errorLocations.get(errorIndex), lineRange);
    }

    private static String getSourceText(JsonObject oldNode) {
        return oldNode.getAsJsonObject("codedata").get("sourceCode").getAsString();
    }

    private LineRange getLineRange(JsonObject jsonObject) {
        return gson.fromJson(jsonObject.get("codedata").getAsJsonObject().get("lineRange"), LineRange.class);
    }

    @Deprecated
    public void markSuggestedNodes(JsonArray oldNodes, JsonArray newNodes, int startIndex) {
        int oldIndex = startIndex;
        int newIndex = startIndex;

        while (oldIndex < oldNodes.size() && newIndex < newNodes.size()) {
            JsonObject oldNode = oldNodes.get(oldIndex).getAsJsonObject();
            JsonObject newNode = newNodes.get(newIndex).getAsJsonObject();

            if (getSourceText(oldNode).equals(getSourceText(newNode))) {
                newNode.addProperty("suggested", false);
                oldIndex++;
                newIndex++;
                continue;
            }

            boolean oldNodeHasBranches = oldNode.has("branches");
            boolean newNodeHasBranches = newNode.has("branches");
            if (oldNodeHasBranches && newNodeHasBranches) {
                markBranches(oldNode.getAsJsonArray("branches"), newNode.getAsJsonArray("branches"));
                oldIndex++;
                newIndex++;
                continue;
            }

            handleSuggestedNode(newNodes, newIndex, newNode);
            newIndex++;
        }

        while (newIndex < newNodes.size()) {
            handleSuggestedNode(newNodes, newIndex, newNodes.get(newIndex).getAsJsonObject());
            newIndex++;
        }
    }

    private void markBranches(JsonArray oldBranches, JsonArray newBranches) {
        for (int i = 0; i < newBranches.size(); i++) {
            JsonObject newBranch = newBranches.get(i).getAsJsonObject();
            String newLabel = newBranch.get("label").getAsString();
            boolean labelMatched = false;

            for (int j = 0; j < oldBranches.size(); j++) {
                JsonObject oldBranch = oldBranches.get(j).getAsJsonObject();
                if (oldBranch.get("label").getAsString().equals(newLabel)) {
                    markSuggestedNodes(oldBranch.getAsJsonArray("children"), newBranch.getAsJsonArray("children"), 0);
                    labelMatched = true;
                    break;
                }
            }

            if (!labelMatched) {
                newBranch.addProperty("suggested", true);
            }
        }
    }
}
