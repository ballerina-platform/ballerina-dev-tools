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
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MarkdownDocumentationNode;
import io.ballerina.compiler.syntax.tree.MarkdownParameterDocumentationLineNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.RestParameterNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.flowmodelgenerator.core.Constants;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        FunctionDocumentation documentation = getFunctionDocumentation(functionDefinitionNode);

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
            default -> FunctionDefinitionBuilder.setMandatoryProperties(nodeBuilder, returnType,
                    documentation == null ? "" : documentation.description(),
                    documentation == null ? "" : documentation.returnDescription());
        }

        boolean isContextParamAvailable = false;
        String npPromptDefaultValue = null;

        // TODO: Check how we can do this in a cleaner way
        LineRange promptLineRange = null;

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
            if (isNpFunctionProperty(parameter)) {
                if (Constants.NaturalFunctions.CONTEXT.equals(paramName.get().text())) {
                    isContextParamAvailable = true;
                } else if (Constants.NaturalFunctions.PROMPT.equals(paramName.get().text())) {
                    npPromptDefaultValue = ((DefaultableParameterNode) parameter).expression().toSourceCode();
                    promptLineRange = parameter.lineRange();
                }

                continue;
            }

            String paramNameText = paramName.map(Token::text).orElse("");
            Token paramToken = paramName.orElse(null);
            switch (nodeKind) {
                case AUTOMATION -> AutomationBuilder.setProperty(nodeBuilder.properties(), paramType, paramNameText,
                        paramToken);
                case DATA_MAPPER_DEFINITION -> DataMapperDefinitionBuilder.setProperty(nodeBuilder.properties(),
                        paramType, paramNameText, paramToken);
                default -> {
                    String paramDescription = "";
                    if (documentation != null) {
                        String paramDesc = documentation.parameterDescriptions().get(paramNameText);
                        if (paramDesc != null) {
                            paramDescription = paramDesc;
                        }
                    }
                    FunctionDefinitionBuilder.setProperty(nodeBuilder.properties(), paramType, paramNameText,
                            paramDescription, paramToken);
                }
            }
        }

        switch (nodeKind) {
            case DATA_MAPPER_DEFINITION -> DataMapperDefinitionBuilder.setOptionalProperties(nodeBuilder);
            case AUTOMATION -> AutomationBuilder.setOptionalProperties(nodeBuilder, !returnType.isEmpty());
            case NP_FUNCTION_DEFINITION -> {
                NPFunctionDefinitionBuilder.endOptionalProperties(nodeBuilder);
                processNpFunctionDefinitionProperties(nodeBuilder, npPromptDefaultValue, promptLineRange,
                        isContextParamAvailable);
            }
            default -> FunctionDefinitionBuilder.setOptionalProperties(nodeBuilder);
        }

        Optional<MetadataNode> optMetadata = functionDefinitionNode.metadata();
        if (optMetadata.isPresent()) {
            StringBuilder annot = new StringBuilder();
            NodeList<AnnotationNode> annotations = optMetadata.get().annotations();
            for (AnnotationNode annotation : annotations) {
                annot.append(annotation.toSourceCode());
            }
            annot.append(System.lineSeparator());
            nodeBuilder.properties().annotations(annot.toString());
        }

        for (Token token : functionDefinitionNode.qualifierList()) {
            if (token.text().equals("isolated")) {
                nodeBuilder.properties().isIsolated(true, true, false, false);
            }
        }

        // Build the definition node
        this.node = gson.toJsonTree(nodeBuilder.build());
    }

    private void processNpFunctionDefinitionProperties(NodeBuilder nodeBuilder, String promptDefaultValue,
                                                       LineRange promptLineRange, boolean isContextAvailable) {
        // Set the 'prompt' property
        nodeBuilder.properties().custom()
                .metadata()
                    .label(Constants.NaturalFunctions.PROMPT_LABEL)
                    .description(Constants.NaturalFunctions.PROMPT_DESCRIPTION)
                    .stepOut()
                .codedata()
                    .kind(ParameterData.Kind.REQUIRED.name())
                    .lineRange(promptLineRange)
                    .stepOut()
                .typeConstraint(Constants.NaturalFunctions.MODULE_PREFIXED_PROMPT_TYPE)
                .value(promptDefaultValue)
                .editable()
                .hidden()
                .type(Property.ValueType.RAW_TEMPLATE)
                .stepOut()
                .addProperty(Constants.NaturalFunctions.PROMPT);

        // Set the `context` property if enabled
        if (isContextAvailable) {
            nodeBuilder.properties().custom()
                    .metadata()
                        .label(Constants.NaturalFunctions.CONTEXT_LABEL)
                        .description(Constants.NaturalFunctions.CONTEXT_DESCRIPTION)
                        .stepOut()
                    .codedata()
                        .kind(ParameterData.Kind.REQUIRED.name())
                    .stepOut()
                    .typeConstraint(Constants.NaturalFunctions.MODULE_PREFIXED_CONTEXT_TYPE)
                    .editable()
                    .optional(true)
                    .advanced(true)
                    .hidden()
                    .type(Property.ValueType.EXPRESSION)
                    .stepOut()
                    .addProperty(Constants.NaturalFunctions.CONTEXT);
        }

        // set the `enableModelContext` property
        nodeBuilder.properties().custom()
                .metadata()
                    .label(Constants.NaturalFunctions.ENABLE_MODEL_CONTEXT_LABEL)
                    .description(Constants.NaturalFunctions.ENABLE_MODEL_CONTEXT_DESCRIPTION)
                    .stepOut()
                .editable()
                .value(isContextAvailable)
                .optional(true)
                .advanced(true)
                .type(Property.ValueType.FLAG)
                .stepOut()
                .addProperty(Constants.NaturalFunctions.ENABLE_MODEL_CONTEXT);
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

        if (paramName.isEmpty() ||
                (!Constants.NaturalFunctions.PROMPT.equals(paramName.get().text())
                        && !Constants.NaturalFunctions.CONTEXT.equals(paramName.get().text()))) {
            return false;
        }

        Optional<Symbol> paramSymbol = this.semanticModel.symbol(parameterNode);
        if (paramSymbol.isEmpty()) {
            return false;
        }

        TypeSymbol typeDesc = ((ParameterSymbol) paramSymbol.get()).typeDescriptor();
        return CommonUtils.isNpModule(typeDesc) && typeDesc.getName().isPresent()
                && (Constants.NaturalFunctions.PROMPT_TYPE_NAME.equals(typeDesc.getName().get())
                    || Constants.NaturalFunctions.CONTEXT_TYPE_NAME.equals(typeDesc.getName().get()));
    }

    private FunctionDocumentation getFunctionDocumentation(FunctionDefinitionNode funcDefNode) {
        Optional<MetadataNode> optMetadata = funcDefNode.metadata();
        if (optMetadata.isEmpty()) {
            return null;
        }
        Optional<Node> optDocStr = optMetadata.get().documentationString();
        if (optDocStr.isEmpty() || optDocStr.get().kind() != SyntaxKind.MARKDOWN_DOCUMENTATION) {
            return null;
        }
        MarkdownDocumentationNode docNode = (MarkdownDocumentationNode) optDocStr.get();
        StringBuilder description = new StringBuilder();
        Map<String, String> params = new HashMap<>();
        String returnDescription = "";
        for (Node documentationLine : docNode.documentationLines()) {
            if (documentationLine.kind() == SyntaxKind.MARKDOWN_DOCUMENTATION_LINE) {
                NodeList<Node> nodes = ((MarkdownDocumentationLineNode) documentationLine).documentElements();
                if (nodes.size() == 1) {
                    description.append(nodes.get(0).toSourceCode());
                }
            } else if (documentationLine.kind() == SyntaxKind.MARKDOWN_PARAMETER_DOCUMENTATION_LINE) {
                MarkdownParameterDocumentationLineNode docLine =
                        (MarkdownParameterDocumentationLineNode) documentationLine;
                String param = docLine.parameterName().text().trim();
                NodeList<Node> nodes = docLine.documentElements();
                if (!nodes.isEmpty()) {
                    params.put(param, nodes.get(0).toSourceCode());
                }
            } else if (documentationLine.kind() == SyntaxKind.MARKDOWN_RETURN_PARAMETER_DOCUMENTATION_LINE) {
                MarkdownParameterDocumentationLineNode returnDocLine =
                        (MarkdownParameterDocumentationLineNode) documentationLine;
                NodeList<Node> nodes = returnDocLine.documentElements();
                if (!nodes.isEmpty()) {
                    returnDescription = nodes.get(0).toSourceCode();
                }
            }
        }
        return new FunctionDocumentation(description.toString(), params, returnDescription);
    }

    private record FunctionDocumentation (String description, Map<String, String> parameterDescriptions,
                                          String returnDescription) {

    }
}
