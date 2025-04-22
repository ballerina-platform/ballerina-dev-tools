/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.request.FunctionSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceDesignerDiagnosticRequest;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import static io.ballerina.servicemodelgenerator.extension.diagnostics.ResourceFunctionFormValidator.Context.ADD_RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.diagnostics.ResourceFunctionFormValidator.Context.UPDATE_RESOURCE;

/**
 * Diagnostics handler for the service model generator.
 *
 * @since 2.3.0
 */
public class DiagnosticsHandler {

    final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final WorkspaceManager workspaceManager;

    public DiagnosticsHandler(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public JsonElement getDiagnostics(ServiceDesignerDiagnosticRequest request) throws WorkspaceDocumentException,
            EventSyncException {
        switch (request.operation()) {
            case "addResource" -> {
                FunctionSourceRequest function = gson.fromJson(request.request(), FunctionSourceRequest.class);
                new ResourceFunctionFormValidator(workspaceManager, ADD_RESOURCE).validate(function);
                return gson.toJsonTree(function);
            }
            case "updateFunction" -> {
                FunctionSourceRequest function = gson.fromJson(request.request(), FunctionSourceRequest.class);
                new ResourceFunctionFormValidator(workspaceManager, UPDATE_RESOURCE).validate(function);
                return gson.toJsonTree(function);
            }
        }
        return new JsonObject();
    }

    public static NonTerminalNode findNonTerminalNode(Codedata codedata, Document document) {
        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        TextDocument textDocument = syntaxTree.textDocument();
        LineRange lineRange = codedata.getLineRange();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return modulePartNode.findNode(TextRange.from(start, end - start), true);
    }
}
