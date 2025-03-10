/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Generates source code from the flow model.
 *
 * @since 2.0.0
 */
public class SourceGenerator {

    private final Gson gson;
    private final WorkspaceManager workspaceManager;
    private final Path filePath;

    public SourceGenerator(WorkspaceManager workspaceManager, Path filePath) {
        this.workspaceManager = workspaceManager;
        this.filePath = filePath;
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * Converts the flow model to source code.
     *
     * @param diagramNode The flow model node to be converted.
     * @return The source code of the flow model node.
     */
    public JsonElement toSourceCode(JsonElement diagramNode) {
        FlowNode flowNode = gson.fromJson(diagramNode, FlowNode.class);
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, workspaceManager, filePath);
        Map<Path, List<TextEdit>> textEdits =
                NodeBuilder.getNodeFromKind(flowNode.codedata().node()).toSource(sourceBuilder);
        addNewLine(textEdits);
        return gson.toJsonTree(textEdits);
    }

    // If text edit add new change, add new line to the text edit
    private void addNewLine(Map<Path, List<TextEdit>> textEdits) {
        for (Map.Entry<Path, List<TextEdit>> pathListEntry : textEdits.entrySet()) {
            List<TextEdit> edits = pathListEntry.getValue();
            for (TextEdit edit : edits) {
                Range range = edit.getRange();
                if (!range.getStart().equals(new Position(0, 0)) && range.getStart().equals(range.getEnd())) {
                    edit.setNewText(System.lineSeparator() + edit.getNewText());
                    break;
                }
            }
        }
    }
}
