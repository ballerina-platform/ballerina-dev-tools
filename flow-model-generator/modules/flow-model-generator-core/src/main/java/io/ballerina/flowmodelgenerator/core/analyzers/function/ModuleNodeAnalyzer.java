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

package io.ballerina.flowmodelgenerator.core.analyzers.function;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ExternalFunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
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
import io.ballerina.flowmodelgenerator.core.model.node.AutomationBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.NPFunctionDefinitionBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Analyzes the module level functions and generates the flow model.
 *
 * @since 2.0.0
 */
public class ModuleNodeAnalyzer extends NodeVisitor {

    private final ModuleInfo moduleInfo;
    private final SemanticModel semanticModel;
    private final Gson gson;
    private JsonElement node;

    public ModuleNodeAnalyzer(ModuleInfo moduleInfo, SemanticModel semanticModel) {
        this.moduleInfo = moduleInfo;
        this.semanticModel = semanticModel;
        this.gson = new Gson();
    }

    public Optional<JsonElement> findFunction(ModulePartNode rootNode, String functionName) {
        for (ModuleMemberDeclarationNode member : rootNode.members()) {
            if (member.kind() != SyntaxKind.FUNCTION_DEFINITION) {
                continue;
            }

            FunctionDefinitionNode functionNode = (FunctionDefinitionNode) member;
            if (functionNode.functionName().text().equals(functionName)) {
                functionNode.accept(this);
                return Optional.of(this.node);
            }

        }
        return Optional.empty();
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        boolean isNpFunction = isNpFunction(functionDefinitionNode);

        NodeKind nodeKind;
        if (functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY) {
            nodeKind = NodeKind.DATA_MAPPER_DEFINITION;
        } else if (functionDefinitionNode.functionName().text().equals(AutomationBuilder.MAIN_FUNCTION_NAME)) {
            nodeKind = NodeKind.AUTOMATION;
        } else if (isNpFunction) {
            nodeKind = NodeKind.NP_FUNCTION_DEFINITION;
        } else {
            nodeKind = NodeKind.FUNCTION_DEFINITION;
        }

        NodeBuilder nodeBuilder = NodeBuilder.getNodeFromKind(nodeKind)
                .defaultModuleName(this.moduleInfo);

        // Set the line range of the function definition node
        LineRange functionLineRange = functionDefinitionNode.lineRange();
        nodeBuilder.codedata().lineRange(LineRange.from(
                functionLineRange.fileName(),
                functionLineRange.startLine(),
                functionDefinitionNode.functionBody().lineRange().startLine()));

        // Set the function name, return type and nested properties
        String returnType = functionDefinitionNode.functionSignature().returnTypeDesc()
                .map(type -> type.type().toSourceCode().strip())
                .orElse("");
        if (nodeKind != NodeKind.AUTOMATION) {
            nodeBuilder.properties().functionName(functionDefinitionNode.functionName());
        }
        // TODO: Check how we can do this using FunctionDefinitionBuilder as the super class
        switch (nodeKind) {
            case DATA_MAPPER_DEFINITION -> DataMapperDefinitionBuilder.setMandatoryProperties(nodeBuilder, returnType);
            case AUTOMATION -> AutomationBuilder.sendMandatoryProperties(nodeBuilder);
            case NP_FUNCTION_DEFINITION -> NPFunctionDefinitionBuilder.setMandatoryProperties(nodeBuilder, returnType);
            default -> FunctionDefinitionBuilder.setMandatoryProperties(nodeBuilder, returnType);
        }

        // Set the function parameters
        for (ParameterNode parameter : functionDefinitionNode.functionSignature().parameters()) {
            if (isNpFunctionProperty(parameter)) {
                continue;
            }

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
            String paramNameText = paramName.map(Token::text).orElse("");
            Token paramToken = paramName.orElse(null);
            switch (nodeKind) {
                case AUTOMATION -> AutomationBuilder.setProperty(nodeBuilder.properties(), paramType,
                        paramNameText, paramToken);
                case DATA_MAPPER_DEFINITION ->
                        DataMapperDefinitionBuilder.setProperty(nodeBuilder.properties(), paramType,
                                paramNameText, paramToken);
                default -> FunctionDefinitionBuilder.setProperty(nodeBuilder.properties(), paramType,
                        paramNameText, paramToken);
            }
        }

        switch (nodeKind) {
            case DATA_MAPPER_DEFINITION -> DataMapperDefinitionBuilder.setOptionalProperties(nodeBuilder);
            case AUTOMATION -> AutomationBuilder.setOptionalProperties(nodeBuilder, !returnType.isEmpty());
            case NP_FUNCTION_DEFINITION -> {
                NPFunctionDefinitionBuilder.endOptionalProperties(nodeBuilder);
                processNpFunctionDefinitionProperties(functionDefinitionNode, nodeBuilder);
            }
            default -> FunctionDefinitionBuilder.setOptionalProperties(nodeBuilder);
        }

        // Build the definition node
        this.node = gson.toJsonTree(nodeBuilder.build());
    }

    private void processNpFunctionDefinitionProperties(FunctionDefinitionNode functionDefinitionNode,
                                                       NodeBuilder nodeBuilder) {
        AtomicReference<String> npPromptDefaultValue = new AtomicReference<>();
        AtomicReference<String> npModelDefaultValue = new AtomicReference<>();
        AtomicBoolean isModelPropertyAvailable = new AtomicBoolean(false);

        functionDefinitionNode.functionSignature().parameters().forEach(param -> {
            if (param.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
                DefaultableParameterNode defParam = (DefaultableParameterNode) param;
                if (defParam.paramName().isEmpty()) {
                    return;
                }
                if (defParam.paramName().get().text().equals("model")) {
                    isModelPropertyAvailable.set(true);
                    npModelDefaultValue.set(defParam.expression().toSourceCode());
                } else if (defParam.paramName().get().text().equals("prompt")) {
                    npPromptDefaultValue.set(defParam.expression().toSourceCode());
                }
            }
        });

        // Set the NP function properties
        nodeBuilder.properties().custom()
                .metadata()
                    .label(NPFunctionDefinitionBuilder.PROMPT_LABEL)
                    .description(NPFunctionDefinitionBuilder.PROMPT_DESCRIPTION)
                    .stepOut()
                .codedata()
                    .kind(ParameterData.Kind.REQUIRED.name())
                    .stepOut()
                .typeConstraint(NPFunctionDefinitionBuilder.PROMPT_TYPE)
                .value(npPromptDefaultValue.get())
                .editable()
                .hidden()
                .type(Property.ValueType.RAW_TEMPLATE)
                .stepOut()
                .addProperty(NPFunctionDefinitionBuilder.PROMPT);

        if (isModelPropertyAvailable.get()) {
            nodeBuilder.properties().custom()
                    .metadata()
                        .label(NPFunctionDefinitionBuilder.MODEL_LABEL)
                        .description(NPFunctionDefinitionBuilder.MODEL_DESCRIPTION)
                        .stepOut()
                    .codedata()
                        .kind(ParameterData.Kind.DEFAULTABLE.name())
                        .stepOut()
                    .typeConstraint(NPFunctionDefinitionBuilder.MODEL_TYPE)
                    .value(npModelDefaultValue.get())
                    .editable()
                    .optional(true)
                    .advanced(true)
                    .type(Property.ValueType.EXPRESSION)
                    .stepOut()
                    .addProperty(NPFunctionDefinitionBuilder.MODEL);
        }

    }

    private static String getNodeValue(Node node) {
        return node.toSourceCode().strip();
    }

    public JsonElement getNode() {
        return this.node;
    }

    // Utils

    /**
     * Check whether the given function is a prompt as code function.
     *
     * @param functionDefinitionNode Function definition node
     * @return true if the function is a prompt as code function else false
     */
    private boolean isNpFunction(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> funcSymbol = this.semanticModel.symbol(functionDefinitionNode);

        if (funcSymbol.isEmpty() || funcSymbol.get().kind() != SymbolKind.FUNCTION
                || !((FunctionSymbol) funcSymbol.get()).external()) {
            return false;
        }

        return CommonUtils.isNpFunction(((ExternalFunctionSymbol) funcSymbol.get()));
    }

    /**
     * Check whether a particular function parameter is a NP function property. e.g. np:Prompt and np:Model are NP
     * function properties.
     *
     * @return true if the function parameter is a NP function property else false
     */
    private boolean isNpFunctionProperty(ParameterNode parameterNode) {
        List<String> npFunctionProperties = List.of(NPFunctionDefinitionBuilder.PROMPT,
                NPFunctionDefinitionBuilder.MODEL);
        List<String> npFunctionPropertyTypes = List.of("Prompt", "Model");

        Optional<Token> paramName;
        if (parameterNode.kind() == SyntaxKind.REQUIRED_PARAM) {
            RequiredParameterNode reqParam = (RequiredParameterNode) parameterNode;
            paramName = reqParam.paramName();
        } else if (parameterNode.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
            DefaultableParameterNode defParam = (DefaultableParameterNode) parameterNode;
            paramName = defParam.paramName();
        } else {
            return false;
        }

        if (paramName.isEmpty() || !npFunctionProperties.contains(paramName.get().text())) {
            return false;
        }

        Optional<Symbol> paramSymbol = this.semanticModel.symbol(parameterNode);
        if (paramSymbol.isEmpty()) {
            return false;
        }

        TypeSymbol typeDesc = ((ParameterSymbol) paramSymbol.get()).typeDescriptor();
        return CommonUtils.isNpModule(typeDesc) && typeDesc.getName().isPresent()
                && npFunctionPropertyTypes.contains(typeDesc.getName().get());
    }
}
