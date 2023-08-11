/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.core.generators.entrypoint.nodevisitors;

import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;
import io.ballerina.architecturemodelgenerator.core.model.service.Connection;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.findNode;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getClientModuleName;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getSourceLocation;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getReferredClassSymbol;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getReferredNode;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getServiceAnnotation;

/**
 * Build entry point model based on a given Ballerina package.
 *
 * @since 2201.5.1
 */
public class FunctionEntryPointMemberNodeVisitor extends NodeVisitor {

    private final SemanticModel semanticModel;
    private final SyntaxTree syntaxTree;
    private final List<Connection> dependencies = new LinkedList<>();
    private final Path filePath;

    public FunctionEntryPointMemberNodeVisitor(SemanticModel semanticModel, SyntaxTree syntaxTree, Path filePath) {
        this.semanticModel = semanticModel;
        this.syntaxTree = syntaxTree;
        this.filePath = filePath;
    }

    public List<Connection> getDependencies() {
        return dependencies;
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        if (hasInvocationReferences(variableDeclarationNode)) {
            return;
        }
        Node fieldTypeName = getReferredNode(variableDeclarationNode.typedBindingPattern().typeDescriptor());
        if (fieldTypeName != null) {
            Optional<Symbol> fieldTypeNameSymbol = semanticModel.symbol(fieldTypeName);
            if (fieldTypeNameSymbol.isPresent()) {
                ClassSymbol referredClassSymbol = getReferredClassSymbol((TypeSymbol) fieldTypeNameSymbol.get());
                if (referredClassSymbol != null) {
                    boolean isClientClass = referredClassSymbol.qualifiers().stream()
                            .anyMatch(qualifier -> qualifier.equals(Qualifier.CLIENT));
                    if (isClientClass) {
                        DisplayAnnotation displayAnnotation =
                                getServiceAnnotation(variableDeclarationNode.annotations(), filePath.toString());
                        String serviceId = displayAnnotation.getId() != null ? displayAnnotation.getId() :
                                Integer.toString(variableDeclarationNode.hashCode());
                        Connection dependency = new Connection(serviceId,
                                getClientModuleName(referredClassSymbol),
                                getSourceLocation(filePath.toString(), variableDeclarationNode.lineRange()),
                                Collections.emptyList());
                        dependencies.add(dependency);
                    }
                }
            }
        }
    }

    private boolean hasInvocationReferences(VariableDeclarationNode variableDeclarationNode) {
        Optional<Symbol> variableDeclarationNodeSymbol = semanticModel.symbol(variableDeclarationNode);
        if (variableDeclarationNodeSymbol.isEmpty()) {
            return false;
        }
        List<LineRange> objFieldNodeRefs = semanticModel.references(variableDeclarationNodeSymbol.get())
                .stream().map(Location::lineRange).collect(Collectors.toList());
        for (LineRange lineRange : objFieldNodeRefs) {
            Node referredNode = findNode(syntaxTree, lineRange);
            while (!referredNode.kind().equals(SyntaxKind.FUNCTION_DEFINITION) &&
                    !referredNode.kind().equals(SyntaxKind.MODULE_PART)) {
                if (referredNode.kind().equals(SyntaxKind.REMOTE_METHOD_CALL_ACTION) ||
                        referredNode.kind().equals(SyntaxKind.CLIENT_RESOURCE_ACCESS_ACTION)) {
                    return true;
                }
                referredNode = referredNode.parent();
            }
        }
        return false;
    }
}
