package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.PositionUtil;

import java.util.List;

public class SuggestedNodesGenerator {

    private final List<LineRange> errorLocations;
    private JsonArray outputNodes;
    private boolean foundError;
    private int errorIndex;
    private final Gson gson;

    public SuggestedNodesGenerator(List<LineRange> errorLocations) {
        this.errorLocations = errorLocations;
        this.outputNodes = new JsonArray();
        this.foundError = false;
        this.errorIndex = 0;
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
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
            if (oldNodeHasBranches && newNodeHasBranches) {
                markBranches(oldNode.getAsJsonArray("branches"), newNode.getAsJsonArray("branches"));
                oldIndex++;
                newIndex++;
                continue;
            }

            removeIfFoundError(newNodes, newIndex, newNode);
            newIndex++;
        }

        while (newIndex < newNodes.size()) {
            removeIfFoundError(newNodes, newIndex, newNodes.get(newIndex).getAsJsonObject());
            newIndex++;
        }

        this.outputNodes = newNodes;
    }

    private void removeIfFoundError(JsonArray newNodes, int newIndex, JsonObject newNode) {
        if (errorLocations.isEmpty() || !foundError && !isErrorInNode(newNode)) {
            newNode.addProperty("suggested", true);
            return;
        }
        newNodes.remove(newIndex);
        if (!foundError) {
            foundError = true;
            errorIndex++;
        }
    }

    private boolean isErrorInNode(JsonObject newNode) {
        LineRange lineRange =
                gson.fromJson(newNode.get("codedata").getAsJsonObject().get("lineRange"), LineRange.class);
        return PositionUtil.isWithinLineRange(errorLocations.get(errorIndex), lineRange);
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

    private static String getSourceText(JsonObject oldNode) {
        return oldNode.getAsJsonObject("codedata").get("sourceCode").getAsString();
    }

    public JsonArray getNodes() {
        return outputNodes;
    }
}
