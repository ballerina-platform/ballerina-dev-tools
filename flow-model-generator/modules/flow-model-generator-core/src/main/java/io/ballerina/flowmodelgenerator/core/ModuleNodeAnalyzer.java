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
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.model.ModuleInfo;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;

import java.util.Optional;

/**
 * Analyzes the module level functions and generates the flow model.
 *
 * @since 2.0.0
 */
public class ModuleNodeAnalyzer extends NodeVisitor {

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
        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.FUNCTION_DEFINITION)
                .defaultModuleName(this.moduleInfo);

        // Set the function name and return type
        nodeBuilder.properties()
                .functionName(functionDefinitionNode.functionName().text())
                .returnType(functionDefinitionNode.functionSignature().returnTypeDesc()
                        .map(type -> type.type().toSourceCode().strip()).orElse(""));

        // Set the function parameters
        SeparatedNodeList<ParameterNode> parameters = functionDefinitionNode.functionSignature().parameters();
        for (ParameterNode parameter : parameters) {
            String paramType;
            Optional<Token> paramName;
            switch (parameter.kind()) {
                case REQUIRED_PARAM -> {
                    RequiredParameterNode requiredParameter = (RequiredParameterNode) parameter;
                    paramType = getNodeValue(requiredParameter.typeName());
                    paramName = requiredParameter.paramName();
                }
                case DEFAULTABLE_PARAM -> {
                    DefaultableParameterNode defaultableParameter = (DefaultableParameterNode) parameter;
                    paramType = getNodeValue(defaultableParameter.typeName());
                    paramName = defaultableParameter.paramName();
                }
                case REST_PARAM -> {
                    RestParameterNode restParameter = (RestParameterNode) parameter;
                    paramType = getNodeValue(restParameter.typeName()) + restParameter.ellipsisToken().text();
                    paramName = restParameter.paramName();
                }
                default -> {
                    continue;
                }
            }
            nodeBuilder.properties().parameter(paramType, paramName.map(Token::text).orElse(""));
        }

        this.node = gson.toJsonTree(nodeBuilder.build());
    }

    private static String getNodeValue(Node node) {
        return node.toSourceCode().strip();
    }

    public JsonElement getNode() {
        return this.node;
    }
}
