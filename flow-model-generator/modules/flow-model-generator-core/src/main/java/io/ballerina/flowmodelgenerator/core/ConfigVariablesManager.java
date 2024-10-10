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
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manage the configurable variables.
 *
 * @since 1.4.0
 */
public class ConfigVariablesManager {

    public static final String DEFAULTABLE = "defaultable";
    public static final String LS = System.lineSeparator();
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
        List<ConfigVariable> configVariables = new ArrayList<>();
        for (Node node : modulePartNode.children()) {
            if (node.kind() == SyntaxKind.MODULE_VAR_DECL) {
                ModuleVariableDeclarationNode modVarDeclarationNode = (ModuleVariableDeclarationNode) node;
                if (hasConfigurableQualifier(modVarDeclarationNode)) {
                    configVariables.add(genConfigVariable(modVarDeclarationNode));
                }
            }
        }
        return gson.toJsonTree(configVariables);
    }

    private static boolean hasConfigurableQualifier(ModuleVariableDeclarationNode modVarDeclarationNode) {
        return modVarDeclarationNode.qualifiers()
                .stream().anyMatch(q -> q.text().equals(Qualifier.CONFIGURABLE.getValue()));
    }

    private ConfigVariable genConfigVariable(ModuleVariableDeclarationNode modVarDeclNode) {
        Metadata metadata = new Metadata.Builder<>(null)
                .label("Config variables")
                .build();

        Codedata codedata = new Codedata.Builder<>(null)
                .node(NodeKind.ASSIGN)
                .lineRange(modVarDeclNode.lineRange())
                .build();

        Map<String, Property> properties = new LinkedHashMap<>();
        TypedBindingPatternNode typedBindingPattern = modVarDeclNode.typedBindingPattern();
        properties.put(Property.DATA_TYPE_KEY, property(CONFIG_TYPE, CONFIG_TYPE_DESCRIPTION, Property.ValueType.TYPE
                , typedBindingPattern.typeDescriptor().toSourceCode().trim()));
        properties.put(Property.VARIABLE_KEY, property(CONFIG_NAME, CONFIG_NAME_DESCRIPTION,
                Property.ValueType.IDENTIFIER, typedBindingPattern.bindingPattern().toSourceCode().trim()));
        Optional<ExpressionNode> optInitializer = modVarDeclNode.initializer();
        String value = "";
        if (optInitializer.isPresent()) {
            ExpressionNode initializer = optInitializer.get();
            if (initializer.kind() != SyntaxKind.REQUIRED_EXPRESSION) {
                value = initializer.toSourceCode();
            }
        }
        properties.put(DEFAULTABLE, property(DEFAULT_VALUE, DEFAULT_VALUE_DESCRIPTION,
                Property.ValueType.EXPRESSION, value));

        return new ConfigVariable(metadata, codedata, properties);
    }

    private Property property(String label, String description, Property.ValueType valueType, String value) {
        Property.Builder propertyBuilder = Property.Builder.getInstance();
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(description)
                    .stepOut()
                .type(valueType)
                .value(value)
                .editable();
        return propertyBuilder.build();
    }

    public JsonElement update(Document document, Path configFile, JsonElement configs) {
        List<ConfigVariable> configVariables = gson.fromJson(configs, ConfigVariables.class).configVariables();
        StringBuilder sb = new StringBuilder();
        for (ConfigVariable configVariable : configVariables) {
            Map<String, Property> properties = configVariable.properties();
            String value = properties.get(DEFAULTABLE).toSourceCode();
            if (value.isEmpty()) {
                value = "?";
            }
            String config = String.format("configurable %s %s = %s;",
                    properties.get(Property.DATA_TYPE_KEY).toSourceCode(),
                    properties.get(Property.VARIABLE_KEY).toSourceCode(), value);
            sb.append(config).append(LS);
        }

        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();

        List<TextEdit> textEdits = new ArrayList<>();
        LinePosition startPos = LinePosition.from(modulePartNode.lineRange().endLine().line() + 1, 0);
        textEdits.add(new TextEdit(CommonUtils.toRange(startPos), sb.toString()));
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        textEditsMap.put(configFile, textEdits);
        return gson.toJsonTree(textEditsMap);
    }

    private record ConfigVariables(
            List<ConfigVariable> configVariables
    ) {
    }

    private record ConfigVariable(
            Metadata metadata,
            Codedata codedata,
            Map<String, Property> properties
    ) {
    }
}
