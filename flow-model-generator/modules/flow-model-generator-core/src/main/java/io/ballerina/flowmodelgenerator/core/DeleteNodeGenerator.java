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
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates text edits for the nodes that are requested to delete.
 *
 * @since 1.4.0
 */
public class DeleteNodeGenerator {

    private final Gson gson;
    private final FlowNode flowNode;
    private final Path filePath;

    public DeleteNodeGenerator(JsonElement flowNode, Path filePath) {
        this.gson = new Gson();
        this.flowNode = new Gson().fromJson(flowNode, FlowNode.class);
        this.filePath = filePath;
    }

    public JsonElement getTextEditToDeletedNode() {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        List<TextEdit> textEdits = new ArrayList<>();
        textEdits.add(new TextEdit(CommonUtils.toRange(flowNode.codedata().lineRange()), ""));
        textEditsMap.put(filePath, textEdits);
        return gson.toJsonTree(textEditsMap);
    }
}
