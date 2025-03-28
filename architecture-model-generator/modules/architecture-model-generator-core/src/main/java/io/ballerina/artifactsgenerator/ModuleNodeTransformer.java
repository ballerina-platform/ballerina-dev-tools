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

package io.ballerina.artifactsgenerator;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.ExternalFunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeTransformer;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.List;
import java.util.Optional;

/**
 * Transforms module nodes into artifacts based on the syntax node.
 *
 * @since 2.3.0
 */
public class ModuleNodeTransformer extends NodeTransformer<Optional<Artifact>> {

    private final SemanticModel semanticModel;

    private static final String AUTOMATION_FUNCTION_NAME = "automation";
    private static final String MAIN_FUNCTION_NAME = "main";
    private static final String BALLERINAX_ORG_NAME = "ballerinax";
    private static final String NP_MODULE_NAME = "np";
    private static final String LLM_CALL = "LlmCall";

    public ModuleNodeTransformer(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    @Override
    public Optional<Artifact> transform(FunctionDefinitionNode functionDefinitionNode) {
        Artifact.Builder artifactBuilder = new Artifact.Builder().node(functionDefinitionNode);
        String functionName = functionDefinitionNode.functionName().text();

        if (functionName.equals(MAIN_FUNCTION_NAME)) {
            artifactBuilder
                    .name(AUTOMATION_FUNCTION_NAME)
                    .type(Artifact.Type.AUTOMATION);
        } else if (functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY) {
            artifactBuilder
                    .name(functionName)
                    .type(Artifact.Type.DATA_MAPPER);
        } else {
            artifactBuilder
                    .name(functionName)
                    .type(Artifact.Type.FUNCTION);
        }
        return Optional.of(artifactBuilder.build());
    }

    @Override
    protected Optional<Artifact> transformSyntaxNode(Node node) {
        return Optional.empty();
    }

    /**
     * Check whether the given function is a prompt as code function.
     *
     * @param functionDefinitionNode Function definition node
     * @return true if the function is a prompt as code function else false
     */
    private boolean isPromptAsCodeFunction(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> funcSymbol = this.semanticModel.symbol(functionDefinitionNode);
        if (funcSymbol.isEmpty() || !((FunctionSymbol) funcSymbol.get()).external()) {
            return false;
        }

        List<AnnotationAttachmentSymbol> annotAttachments =
                ((ExternalFunctionSymbol) funcSymbol.get()).annotAttachmentsOnExternal();
        return annotAttachments.stream().anyMatch(annot ->
                isNpModule(annot.typeDescriptor())
                        && annot.getName().isPresent()
                        && annot.getName().get().equals(LLM_CALL));
    }

    private boolean isNpModule(Symbol symbol) {
        Optional<ModuleSymbol> module = symbol.getModule();
        if (module.isEmpty()) {
            return false;
        }

        ModuleID moduleId = module.get().id();
        return moduleId.orgName().equals(BALLERINAX_ORG_NAME) && moduleId.packageName().equals(NP_MODULE_NAME);
    }
}
