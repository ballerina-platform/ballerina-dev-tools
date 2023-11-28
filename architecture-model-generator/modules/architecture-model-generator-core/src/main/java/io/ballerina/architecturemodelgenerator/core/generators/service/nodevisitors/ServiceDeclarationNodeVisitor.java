/*
 *  Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.core.generators.service.nodevisitors;

import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticMessage;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticNode;
import io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils;
import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;
import io.ballerina.architecturemodelgenerator.core.model.service.Connection;
import io.ballerina.architecturemodelgenerator.core.model.service.Service;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ClassDefinitionNode;
import io.ballerina.compiler.syntax.tree.EnumDeclarationNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.ballerina.architecturemodelgenerator.core.Constants.DEFAULT_SERVICE_BASE_PATH;
import static io.ballerina.architecturemodelgenerator.core.Constants.FORWARD_SLASH;
import static io.ballerina.architecturemodelgenerator.core.Constants.LISTENER;

/**
 * Visitor class for ServiceDeclaration nodes.
 *
 * @since 2201.2.2
 */
public class ServiceDeclarationNodeVisitor extends NodeVisitor {
    private final PackageCompilation packageCompilation;
    private final SemanticModel semanticModel;
    private final SyntaxTree syntaxTree;
    private final Package currentPackage;
    private final List<Service> services = new LinkedList<>();
    private final List<Connection> dependencies = new LinkedList<>();
    private final Path filePath;
    private List<String> servicePaths = new ArrayList<>();

    public ServiceDeclarationNodeVisitor(PackageCompilation packageCompilation, SemanticModel semanticModel,
                                         SyntaxTree syntaxTree, Package currentPackage, Path filePath) {
        this.packageCompilation = packageCompilation;
        this.semanticModel = semanticModel;
        this.syntaxTree = syntaxTree;
        this.currentPackage = currentPackage;
        this.filePath = filePath;
    }

    public List<Service> getServices() {
        return services;
    }

    public List<Connection> getDependencies() {
        return dependencies;
    }

    @Override
    public void visit(ServiceDeclarationNode serviceDeclarationNode) {

        DisplayAnnotation serviceAnnotation = new DisplayAnnotation();
        NodeList<Node> serviceNameNodes = serviceDeclarationNode.absoluteResourcePath();

        Optional<MetadataNode> metadataNode = serviceDeclarationNode.metadata();
        if (metadataNode.isPresent()) {
            NodeList<AnnotationNode> annotationNodes = metadataNode.get().annotations();
            serviceAnnotation = GeneratorUtils.getServiceAnnotation(annotationNodes, this.filePath.toString());
        }
        String serviceId = generateServiceId(serviceAnnotation, serviceNameNodes);
        String serviceLabel = generateServiceLabel(serviceAnnotation, serviceNameNodes);

        ServiceMemberFunctionNodeVisitor serviceMemberFunctionNodeVisitor =
                new ServiceMemberFunctionNodeVisitor(serviceId, packageCompilation, semanticModel,
                        syntaxTree, currentPackage, filePath.toString());
        List<ArchitectureModelDiagnostic> diagnostics = new ArrayList<>();
        try {
            serviceDeclarationNode.accept(serviceMemberFunctionNodeVisitor);
        } catch (Exception e) {
            DiagnosticMessage message = DiagnosticMessage.failedToGenerate(DiagnosticNode.SERVICE, e.getMessage());
            ArchitectureModelDiagnostic diagnostic = new ArchitectureModelDiagnostic(
                    message.getCode(), message.getDescription(), message.getSeverity(), null, null
            );
            diagnostics.add(diagnostic);
        }

        List<String> dependencyIDs = new ArrayList<>();
        for (Connection dependency : serviceMemberFunctionNodeVisitor.getDependencies()) {
            dependencyIDs.add(dependency.getId());
        }

        services.add(new Service(serviceId, serviceLabel, getServiceType(serviceDeclarationNode),
                serviceMemberFunctionNodeVisitor.getResourceFunctions(),
                serviceMemberFunctionNodeVisitor.getRemoteFunctions(), serviceAnnotation, dependencyIDs,
                GeneratorUtils.getSourceLocation(filePath.toString(), serviceDeclarationNode.lineRange()),
                diagnostics));
        dependencies.addAll(serviceMemberFunctionNodeVisitor.getDependencies());
    }

    private String getServiceType(ServiceDeclarationNode serviceDeclarationNode) {

        String serviceType = null;
        SeparatedNodeList<ExpressionNode> expressionNodes = serviceDeclarationNode.expressions();
        for (ExpressionNode expressionNode : expressionNodes) {
            if (expressionNode instanceof ExplicitNewExpressionNode) {
                ExplicitNewExpressionNode explicitNewExpressionNode = (ExplicitNewExpressionNode) expressionNode;
                //todo: Implement using semantic model - returns null
                TypeDescriptorNode typeDescriptorNode = explicitNewExpressionNode.typeDescriptor();
                if (typeDescriptorNode instanceof QualifiedNameReferenceNode) {
                    QualifiedNameReferenceNode listenerNode = (QualifiedNameReferenceNode) typeDescriptorNode;
                    Optional<Symbol> listenerSymbol = semanticModel.symbol(listenerNode);
                    if (listenerSymbol.isPresent() && (listenerSymbol.get() instanceof TypeReferenceTypeSymbol)) {
                        serviceType = ((TypeReferenceTypeSymbol)
                                listenerSymbol.get()).signature().replace(LISTENER, "");
                    } else {
                        serviceType = listenerNode.modulePrefix().text().trim();
                    }
                }
            } else if (expressionNode instanceof SimpleNameReferenceNode) { // support when use listener from a var
                Optional<TypeSymbol> typeSymbol = semanticModel.typeOf(expressionNode);
                if (typeSymbol.isPresent() && typeSymbol.get().typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                    serviceType = typeSymbol.get().signature().replace(LISTENER, "");
                }
            }
        }
        return serviceType;
    }

    private boolean isValidUUID(String uuidString) {
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String generateServiceId(DisplayAnnotation annotation, NodeList<Node> serviceNameNodes) {
        String servicePath = getServicePath(serviceNameNodes);
        if (servicePath.isBlank() || servicePath.equals(FORWARD_SLASH)) {
            servicePath = DEFAULT_SERVICE_BASE_PATH;
        }

        String indexedServicePath = servicePath;
        if (servicePaths.contains(servicePath)) {
            int index = getServicePathIndex(servicePath);
            indexedServicePath = servicePath + (index + 1);
        }
        String serviceId = currentPackage.descriptor().org().value() + ":" + currentPackage.descriptor().name().value()
                + ":" + indexedServicePath.replace(FORWARD_SLASH, "_");
        servicePaths.add(servicePath);

        return annotation.getId().isEmpty() ? serviceId : annotation.getId();
    }

    private String generateServiceLabel(DisplayAnnotation annotation, NodeList<Node> serviceNameNodes) {
        String label = annotation.getLabel();
        if (label.isEmpty() || isValidUUID(label)) {
            String servicePath = servicePaths.remove(servicePaths.size() - 1);
            int index = getServicePathIndex(servicePath);
            servicePaths.add(servicePath);
            String rawServicePath = getServicePath(serviceNameNodes);
            if (rawServicePath.isBlank() || rawServicePath.equals(FORWARD_SLASH)) {
                return currentPackage.descriptor().name() + " Component" + (index > 0 ? index + 1 : "");
            }
            return rawServicePath + (index > 0 ? index + 1 : "");
        }

        return label;
    }

    private String getServicePath(NodeList<Node> serviceNameNodes) {
        StringBuilder servicePathBuilder = new StringBuilder();
        for (Node serviceNameNode : serviceNameNodes) {
            servicePathBuilder.append(serviceNameNode.toString().replace("\"", ""));
        }
        return servicePathBuilder.toString().startsWith(FORWARD_SLASH)
                ? servicePathBuilder.substring(1).trim()
                : servicePathBuilder.toString().trim();
    }

    private int getServicePathIndex(String servicePath) {
        List<String> filteredServicePaths = servicePaths.stream()
                .filter(path -> path.equals(servicePath))
                .collect(Collectors.toList());
        return filteredServicePaths.size();
    }

    @Override
    public void visit(ImportDeclarationNode importDeclarationNode) {

    }

    @Override
    public void visit(EnumDeclarationNode enumDeclarationNode) {

    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {

    }

    @Override
    public void visit(TypeDefinitionNode typeDefinitionNode) {

    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {

    }

    @Override
    public void visit(ClassDefinitionNode classDefinitionNode) {

    }
}
