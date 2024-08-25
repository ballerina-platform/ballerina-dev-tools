package io.ballerina.flowmodelgenerator.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;

import java.util.List;

public class SuggestedNodesGenerator {

    private final List<LineRange> errorLocations;
    private JsonArray outputNodes;
    private boolean foundError;

    public SuggestedNodesGenerator(List<LineRange> errorLocations) {
        this.errorLocations = errorLocations;
        this.outputNodes = new JsonArray();
        this.foundError = false;
    }

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
            if (oldNodeHasBranches != newNodeHasBranches) {
                newNode.addProperty("suggested", true);
                newIndex++;
                continue;
            }

            if (oldNodeHasBranches) {
                markBranches(oldNode.getAsJsonArray("branches"), newNode.getAsJsonArray("branches"));
                oldIndex++;
                newIndex++;
            }
        }

        while (newIndex < newNodes.size()) {
            newNodes.get(newIndex).getAsJsonObject().addProperty("suggested", true);
            newIndex++;
        }

        this.outputNodes = newNodes;
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

    private boolean checkErrorRange(JsonObject jsonObject) {
        JsonObject objLineRange = jsonObject.get("codedata").getAsJsonObject().get("lineRange").getAsJsonObject();
        return false;
    }

    private static String getSourceText(JsonObject oldNode) {
        return oldNode.getAsJsonObject("codedata").get("sourceCode").getAsString();
    }

    public JsonArray getNodes() {
        return outputNodes;
    }
}
