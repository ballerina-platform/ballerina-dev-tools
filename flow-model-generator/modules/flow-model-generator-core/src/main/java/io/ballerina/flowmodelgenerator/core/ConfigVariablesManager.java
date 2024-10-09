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
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.*;

/**
 * Generates functions based on a given keyword.
 *
 * @since 1.4.0
 */
public class ConfigVariablesManager {

    private final Gson gson;

    public static final String CONFIG_TYPE = "Config type";
    public static final String CONFIG_TYPE_DESCRIPTION = "Type of the configuration";
    public static final String CONFIG_NAME = "Config name";
    public static final String CONFIG_NAME_DESCRIPTION = "Name of the config variable";
    public static final String DEFAULT_VALUE = "Default value";
    public static final String DEFAULT_VALUE_DESCRIPTION = "Default value for the config, if empty your need to " +
            "provide a value at runtime";

    public ConfigVariablesManager() {
        this.gson = new Gson();
    }

    public JsonElement get(Document document) {
        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        Map<String, Property> properties = new LinkedHashMap<>();
        for (Node node: modulePartNode.children()) {
            if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
                ModuleVariableDeclarationNode modVarDeclarationNode = (ModuleVariableDeclarationNode) node;
                if (hasConfigurableQualifier(modVarDeclarationNode)) {
                    Optional<ExpressionNode> initializer = modVarDeclarationNode.initializer();
                    if (initializer.isEmpty()) {
                        continue;
                    }
                    Metadata metadata = new Metadata(CONFIG_TYPE, CONFIG_NAME_DESCRIPTION, null, null);
                    Property property = new Property(metadata, Property.ValueType.TYPE.name(), null, modVarDeclarationNode.typedBindingPattern().typeDescriptor().toSourceCode(), false, true);
                    properties.put("type", property);
                    property = new Property(metadata, Property.ValueType.IDENTIFIER.name(), null, modVarDeclarationNode.typedBindingPattern().bindingPattern().toString(), false, true);
                    properties.put("variable", property);
                    property = new Property(metadata, Property.ValueType.EXPRESSION.name(), null, initializer.get(), false, true);
                    properties.put("defaultable", property);
                }
            }
        }
        return null;
    }

//    public JsonElement textEditsToAddConfigurableVariables(String variable, String type, String value) throws WorkspaceDocumentException, EventSyncException {
//        Path projectPath = Path.of(projectName);
//        Path configFile = projectPath.resolve(CONFIG_BAL);
//        this.workspaceManager.loadProject(configFile);
//        Optional<Document> document = this.workspaceManager.document(configFile);
//        if (document.isEmpty()) {
//            return null;
//        }
//        SyntaxTree syntaxTree = document.get().syntaxTree();
//        LineRange lineRange = syntaxTree.rootNode().lineRange();
//        String configurableStmt = String.format("configurable %s %s = %s;", type, variable, value) + System.lineSeparator();
//        TextEdit textEdit = new TextEdit(CommonUtils.toRange(lineRange), configurableStmt);
//        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
//        textEditsMap.put(configFile, List.of(textEdit));
//        return gson.toJsonTree(textEditsMap);
//    }

    private static boolean hasConfigurableQualifier(ModuleVariableDeclarationNode modVarDeclarationNode) {
        return modVarDeclarationNode.qualifiers()
                .stream().anyMatch(q -> q.text().equals(Qualifier.CONFIGURABLE.getValue()));
    }

    public record ConfigVariables(
            ConfigVariable[] configVariables
    ) {}

    private record ConfigVariable(
            Metadata metadata,
            Codedata codedata,
            Map<String, Property> properties
    ) {}
}
