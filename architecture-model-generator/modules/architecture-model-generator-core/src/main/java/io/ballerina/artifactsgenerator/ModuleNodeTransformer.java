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
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeTransformer;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private static final String NP_FUNCTION_ANNOTATION = "NaturalFunction";

    public ModuleNodeTransformer(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    @Override
    public Optional<Artifact> transform(FunctionDefinitionNode functionDefinitionNode) {
        Artifact.Builder functionBuilder = new Artifact.Builder(functionDefinitionNode);
        String functionName = functionDefinitionNode.functionName().text();

        if (functionName.equals(MAIN_FUNCTION_NAME)) {
            functionBuilder
                    .name(AUTOMATION_FUNCTION_NAME)
                    .type(Artifact.Type.AUTOMATION);
        } else if (functionDefinitionNode.functionBody().kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY) {
            functionBuilder
                    .name(functionName)
                    .type(Artifact.Type.DATA_MAPPER);
        } else if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            functionBuilder
                    .accessor(functionName)
                    .name(getPathString(functionDefinitionNode.relativeResourcePath()))
                    .type(Artifact.Type.RESOURCE);
        } else if (functionDefinitionNode.qualifierList().stream()
                .anyMatch(qualifier -> qualifier.kind() == SyntaxKind.REMOTE_KEYWORD)) {
            functionBuilder
                    .name(functionName)
                    .type(Artifact.Type.REMOTE);
        } else if (isPromptAsCodeFunction(functionDefinitionNode)) {
            functionBuilder
                    .name(functionName)
                    .type(Artifact.Type.NP_FUNCTION);
        } else {
            functionBuilder
                    .name(functionName)
                    .type(Artifact.Type.FUNCTION);
        }
        return Optional.of(functionBuilder.build());
    }

    @Override
    public Optional<Artifact> transform(ServiceDeclarationNode serviceDeclarationNode) {
        Artifact.Builder serviceBuilder = new Artifact.Builder(serviceDeclarationNode);

        // Generate the service path
        String absolutePath = getPathString(serviceDeclarationNode.absoluteResourcePath());
        serviceBuilder
                .name(absolutePath)
                .type(Artifact.Type.SERVICE);

        // Check for the child functions
        serviceDeclarationNode.members().stream().parallel().forEach(member -> {
            member.apply(this).ifPresent(serviceBuilder::child);
        });

        return Optional.of(serviceBuilder.build());
    }

    private static String getPathString(NodeList<Node> nodes) {
        return nodes.stream()
                .map(node -> node.toString().trim())
                .collect(Collectors.joining());
    }

    @Override
    protected Optional<Artifact> transformSyntaxNode(Node node) {
        return Optional.empty();
    }

    private boolean isPromptAsCodeFunction(FunctionDefinitionNode functionDefinitionNode) {
        Optional<Symbol> funcSymbol = this.semanticModel.symbol(functionDefinitionNode);
        if (funcSymbol.isEmpty() || !((FunctionSymbol) funcSymbol.get()).external()) {
            return false;
        }

        List<AnnotationAttachmentSymbol> annotAttachments = ((ExternalFunctionSymbol) funcSymbol.get())
                .annotAttachmentsOnExternal();
        return annotAttachments.stream()
                .map(AnnotationAttachmentSymbol::typeDescriptor)
                .anyMatch(annot -> isNpModule(annot) && annot.nameEquals(NP_FUNCTION_ANNOTATION));
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
