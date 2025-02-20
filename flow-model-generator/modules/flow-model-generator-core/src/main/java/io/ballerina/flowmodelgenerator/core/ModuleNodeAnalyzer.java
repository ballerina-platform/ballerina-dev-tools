/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.wso2.org/licenses/LICENSE-2.0
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
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionDefinitionBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.tools.text.LineRange;

import java.util.Optional;

/**
 * Analyzes the module level functions and generates the flow model.
 *
 * @since 2.0.0
 */
public class ModuleNodeAnalyzer extends NodeVisitor {

    private static final String MAIN_FUNCTION_NAME = "main";

    private final ModuleInfo moduleInfo;
    private final Gson gson;
    private JsonElement node;

    public ModuleNodeAnalyzer(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
        this.gson = new Gson();
    }

    public Optional<JsonElement> findFunction(ModulePartNode rootNode, String functionName) {
        for (ModuleMemberDeclarationNode member : rootNode.members()) {
            if (member.kind() == SyntaxKind.FUNCTION_DEFINITION) {
                FunctionDefinitionNode functionNode = (FunctionDefinitionNode) member;
                if (functionNode.functionName().text().equals(functionName)) {
                    functionNode.accept(this);
                    return Optional.of(this.node);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        // Build the metadata based on the function body kind
        FunctionMetadata metadata = functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY
                ? new FunctionMetadata(
                    NodeKind.DATA_MAPPER_DEFINITION,
                    DataMapperDefinitionBuilder.PARAMETERS_LABEL,
                    DataMapperDefinitionBuilder.PARAMETERS_DOC,
                    false,
                    DataMapperDefinitionBuilder.RECORD_TYPE)
                : new FunctionMetadata(
                    NodeKind.FUNCTION_DEFINITION,
                    FunctionDefinitionBuilder.PARAMETERS_LABEL,
                    FunctionDefinitionBuilder.PARAMETERS_DOC,
                    true,
                    null);
        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(metadata.nodeKind)
                .defaultModuleName(this.moduleInfo);

        // Set the line range of the function definition node
        LineRange functionKeywordLineRange = functionDefinitionNode.functionKeyword().lineRange();
        nodeBuilder.codedata().lineRange(LineRange.from(
                functionKeywordLineRange.fileName(),
                functionKeywordLineRange.startLine(),
                functionDefinitionNode.functionBody().lineRange().startLine()));

        // Set the function name, return type and nested properties
        String functionNameText = functionDefinitionNode.functionName().text();
        nodeBuilder.properties()
                .functionName(functionNameText, !functionNameText.equals(MAIN_FUNCTION_NAME))
                .returnType(
                        functionDefinitionNode.functionSignature().returnTypeDesc()
                                .map(type -> type.type().toSourceCode().strip())
                                .orElse(""),
                        metadata.returnTypeConstraint,
                        metadata.returnTypeConstraint == null)
                .nestedProperty();

        // Set the function parameters
        for (ParameterNode parameter : functionDefinitionNode.functionSignature().parameters()) {
            String paramType;
            Optional<Token> paramName;
            switch (parameter.kind()) {
                case REQUIRED_PARAM -> {
                    RequiredParameterNode reqParam = (RequiredParameterNode) parameter;
                    paramType = getNodeValue(reqParam.typeName());
                    paramName = reqParam.paramName();
                }
                case DEFAULTABLE_PARAM -> {
                    DefaultableParameterNode defParam = (DefaultableParameterNode) parameter;
                    paramType = getNodeValue(defParam.typeName());
                    paramName = defParam.paramName();
                }
                case REST_PARAM -> {
                    RestParameterNode restParam = (RestParameterNode) parameter;
                    paramType = getNodeValue(restParam.typeName()) + restParam.ellipsisToken().text();
                    paramName = restParam.paramName();
                }
                default -> {
                    continue;
                }
            }
            nodeBuilder.properties().parameter(paramType, paramName.map(Token::text).orElse(""));
        }
        nodeBuilder.properties().endNestedProperty(
                Property.ValueType.REPEATABLE_PROPERTY,
                Property.PARAMETERS_KEY,
                metadata.parametersLabel,
                metadata.parametersDoc,
                FunctionDefinitionBuilder.getParameterSchema(),
                metadata.optionalParameters);

        // Build the definition node
        this.node = gson.toJsonTree(nodeBuilder.build());
    }

    private static String getNodeValue(Node node) {
        return node.toSourceCode().strip();
    }

    public JsonElement getNode() {
        return this.node;
    }

    private record FunctionMetadata(
            NodeKind nodeKind,
            String parametersLabel,
            String parametersDoc,
            boolean optionalParameters,
            String returnTypeConstraint) {
    }
}
