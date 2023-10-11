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

import io.ballerina.architecturemodelgenerator.core.Constants.ParameterIn;
import io.ballerina.architecturemodelgenerator.core.diagnostics.ArchitectureModelDiagnostic;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticMessage;
import io.ballerina.architecturemodelgenerator.core.diagnostics.DiagnosticNode;
import io.ballerina.architecturemodelgenerator.core.model.SourceLocation;
import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;
import io.ballerina.architecturemodelgenerator.core.model.common.FunctionParameter;
import io.ballerina.architecturemodelgenerator.core.model.service.Connection;
import io.ballerina.architecturemodelgenerator.core.model.service.RemoteFunction;
import io.ballerina.architecturemodelgenerator.core.model.service.ResourceFunction;
import io.ballerina.architecturemodelgenerator.core.model.service.ResourceParameter;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ResourcePathParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.findNode;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getClientModuleName;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getSourceLocation;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getReferencedType;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getReferredClassSymbol;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getReferredNode;
import static io.ballerina.architecturemodelgenerator.core.generators.GeneratorUtils.getServiceAnnotation;

/**
 * Visitor class for FunctionDefinition node.
 *
 * @since 2201.2.2
 */
public class ServiceMemberFunctionNodeVisitor extends NodeVisitor {
    private final String serviceId;
    private final PackageCompilation packageCompilation;
    private final SemanticModel semanticModel;
    private final SyntaxTree syntaxTree;
    private final Package currentPackage;
    private List<ResourceFunction> resourceFunctions = new LinkedList<>();
    private List<RemoteFunction> remoteFunctions = new LinkedList<>();
    private final List<Connection> dependencies = new LinkedList<>();
    private final String filePath;

    public ServiceMemberFunctionNodeVisitor(String serviceId, PackageCompilation packageCompilation,
                                            SemanticModel semanticModel, SyntaxTree syntaxTree,
                                            Package currentPackage, String filePath) {
        this.serviceId = serviceId;
        this.packageCompilation = packageCompilation;
        this.semanticModel = semanticModel;
        this.syntaxTree = syntaxTree;
        this.currentPackage = currentPackage;
        this.filePath = filePath;
    }

    public List<ResourceFunction> getResourceFunctions() {
        return resourceFunctions;
    }

    public List<RemoteFunction> getRemoteFunctions() {
        return remoteFunctions;
    }

    public List<Connection> getDependencies() {
        return dependencies;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        SourceLocation elementLocation = getSourceLocation(filePath,
                functionDefinitionNode.lineRange());
        SyntaxKind kind = functionDefinitionNode.kind();
        switch (kind) {
            case RESOURCE_ACCESSOR_DEFINITION: {
                StringBuilder resourcePathBuilder = new StringBuilder();
                List<ResourceParameter> resourceParameterList = new ArrayList<>();
                NodeList<Node> relativeResourcePaths = functionDefinitionNode.relativeResourcePath();
                for (Node path : relativeResourcePaths) {
                    if (path instanceof ResourcePathParameterNode) {
                        ResourcePathParameterNode pathParam = (ResourcePathParameterNode) path;
                        resourceParameterList.add(getPathParameter(pathParam));
                    }
                    resourcePathBuilder.append(path);
                }

                String resourcePath = resourcePathBuilder.toString().trim();
                String method = functionDefinitionNode.functionName().text().trim();
                String resourceId = String.format("%s:%s:%s", serviceId, resourcePath, method);

                getParameters(functionDefinitionNode.functionSignature(), true,
                        resourceParameterList, null);
                List<String> returnTypes = getReturnTypes(functionDefinitionNode);

                ActionNodeVisitor actionNodeVisitor =
                        new ActionNodeVisitor(packageCompilation, semanticModel, currentPackage, filePath);
                List<ArchitectureModelDiagnostic> diagnostics = new ArrayList<>();
                try {
                    functionDefinitionNode.accept(actionNodeVisitor);
                } catch (Exception e) {
                    DiagnosticMessage message = DiagnosticMessage.failedToGenerate(DiagnosticNode.RESOURCE,
                            e.getMessage());
                    ArchitectureModelDiagnostic diagnostic = new ArchitectureModelDiagnostic(
                            message.getCode(), message.getDescription(), message.getSeverity(), null, null
                    );
                    diagnostics.add(diagnostic);
                }

                ResourceFunction resource = new ResourceFunction(resourceId, resourcePath, resourceParameterList,
                        returnTypes, actionNodeVisitor.getInteractionList(), elementLocation, diagnostics);
                resourceFunctions.add(resource);

                break;
            }
            case OBJECT_METHOD_DEFINITION: {
                boolean isRemote = functionDefinitionNode.qualifierList().stream().
                        anyMatch(item -> item.kind().equals(SyntaxKind.REMOTE_KEYWORD));
                if (isRemote) {
                    String name = functionDefinitionNode.functionName().text().trim();
                    List<FunctionParameter> parameterList = new ArrayList<>();
                    getParameters(functionDefinitionNode.functionSignature(),
                            false, null, parameterList);
                    List<String> returnTypes = getReturnTypes(functionDefinitionNode);

                    ActionNodeVisitor actionNodeVisitor = new ActionNodeVisitor(
                            packageCompilation, semanticModel, currentPackage, filePath);
                    List<ArchitectureModelDiagnostic> diagnostics = new ArrayList<>();
                    try {
                        functionDefinitionNode.accept(actionNodeVisitor);
                    } catch (Exception e) {
                        DiagnosticMessage message = DiagnosticMessage.failedToGenerate(DiagnosticNode.REMOTE_FUNCTION,
                                e.getMessage());
                        ArchitectureModelDiagnostic diagnostic = new ArchitectureModelDiagnostic(
                                message.getCode(), message.getDescription(), message.getSeverity(), null, null
                        );
                        diagnostics.add(diagnostic);
                    }

                    String remoteFunctionId = String.format("%s:%s", serviceId, name);
                    RemoteFunction remoteFunction = new RemoteFunction(remoteFunctionId, name, parameterList,
                            returnTypes, actionNodeVisitor.getInteractionList(), elementLocation, diagnostics);
                    remoteFunctions.add(remoteFunction);
                }
                break;
            }
        }
    }

    private ResourceParameter getPathParameter(ResourcePathParameterNode resourcePathParameterNode) {
        SourceLocation elementLocation = getSourceLocation(this.filePath,
                resourcePathParameterNode.lineRange());
        String name = resourcePathParameterNode.paramName().get().text();
        List<String> paramTypes = new LinkedList<>();
        Optional<Symbol> symbol = semanticModel.symbol(resourcePathParameterNode);
        if (symbol.isPresent()) {
            PathParameterSymbol parameterSymbol = ((PathParameterSymbol) symbol.get());
            paramTypes = getReferencedType(parameterSymbol.typeDescriptor(), currentPackage);
        } // todo : implement else
        return new ResourceParameter(paramTypes, name, ParameterIn.PATH.getValue(), true, elementLocation,
                Collections.emptyList());
    }

    private void getParameters(FunctionSignatureNode functionSignatureNode, boolean isResource,
                               List<ResourceParameter> resourceParams, List<FunctionParameter> remoteFunctionParams) {

        SeparatedNodeList<ParameterNode> parameterNodes = functionSignatureNode.parameters();
        for (ParameterNode parameterNode : parameterNodes) {
            SourceLocation elementLocation = getSourceLocation(this.filePath,
                    parameterNode.lineRange());
            Optional<Symbol> symbol = semanticModel.symbol(parameterNode);
            if (symbol.isPresent() && symbol.get().kind().equals(SymbolKind.PARAMETER)) {
                String paramIn = "";
                String paramName = "";
                boolean isRequired = false;
                ParameterSymbol parameterSymbol = ((ParameterSymbol) symbol.get());
                TypeSymbol typeSymbol = parameterSymbol.typeDescriptor();
                List<String> paramTypes = getReferencedType(typeSymbol, currentPackage);
                switch (parameterNode.kind()) {
                    case REQUIRED_PARAM:
                        RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
                        paramIn = getParameterIn(requiredParameterNode.annotations());
                        paramName = requiredParameterNode.paramName().isPresent() ?
                                requiredParameterNode.paramName().get().toString() : "";
                        isRequired = true;
                        break;
                    case DEFAULTABLE_PARAM:
                        DefaultableParameterNode defaultableParameterNode = (DefaultableParameterNode) parameterNode;
                        paramIn = getParameterIn(defaultableParameterNode.annotations());
                        paramName = defaultableParameterNode.paramName().isPresent() ?
                                defaultableParameterNode.paramName().get().toString() : "";
                        break;
                    case INCLUDED_RECORD_PARAM:
                        break;
                    // res params

                }
                if (isResource) {
                    // todo : param kind
                    resourceParams.add(new ResourceParameter(
                            paramTypes, paramName.trim(), paramIn, isRequired, elementLocation,
                            Collections.emptyList()));
                } else {
                    remoteFunctionParams.add(new FunctionParameter(paramTypes, paramName, isRequired, elementLocation,
                            Collections.emptyList()));
                }
            }
        }
    }

    private String getParameterIn(NodeList<AnnotationNode> annotationNodes) {

        String in = "";
        Optional<AnnotationNode> payloadAnnotation = annotationNodes.stream().filter(annot ->
                annot.toString().trim().equals("@http:Payload")).findAny();
        Optional<AnnotationNode> headerAnnotation = annotationNodes.stream().filter(annot ->
                annot.toString().trim().equals("@http:Header")).findAny();
        // do we need to handle http:Request

        if (payloadAnnotation.isPresent()) {
            in = ParameterIn.BODY.getValue();
        } else if (headerAnnotation.isPresent()) {
            in = ParameterIn.HEADER.getValue();
        } else {
            in = ParameterIn.QUERY.getValue();
        }
        return in;
    }

    private List<String> getReturnTypes(FunctionDefinitionNode functionDefinitionNode) {

        List<String> returnTypes = new ArrayList<>();
        FunctionSignatureNode functionSignature = functionDefinitionNode.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDescriptor = functionSignature.returnTypeDesc();
        if (returnTypeDescriptor.isPresent()) {
            Optional<Symbol> symbol = semanticModel.symbol(functionDefinitionNode);
            if (symbol.isPresent() && symbol.get().kind().equals(SymbolKind.METHOD) ||
                    symbol.get().kind().equals(SymbolKind.RESOURCE_METHOD)) {
                MethodSymbol resourceMethodSymbol = (MethodSymbol) symbol.get();
                Optional<TypeSymbol> returnTypeSymbol = resourceMethodSymbol.typeDescriptor().returnTypeDescriptor();
                returnTypeSymbol.ifPresent(typeSymbol ->
                        returnTypes.addAll(getReferencedType(typeSymbol, currentPackage)));
            }
            // need to split by pipe sign
        }
        return returnTypes;
    }

    @Override
    public void visit(ObjectFieldNode objectFieldNode) {
        if (hasInvocationReferences(objectFieldNode)) {
            return;
        }
        Node fieldTypeName = getReferredNode(objectFieldNode.typeName());
        if (fieldTypeName != null) {
            Optional<Symbol> fieldTypeNameSymbol = semanticModel.symbol(fieldTypeName);
            if (fieldTypeNameSymbol.isPresent()) {
                ClassSymbol referredClassSymbol = getReferredClassSymbol((TypeSymbol) fieldTypeNameSymbol.get());
                if (referredClassSymbol != null) {
                    boolean isClientClass = referredClassSymbol.qualifiers().stream()
                            .anyMatch(qualifier -> qualifier.equals(Qualifier.CLIENT));
                    if (isClientClass) {
                        String serviceId = Integer.toString(objectFieldNode.hashCode());
                        if (objectFieldNode.metadata().isPresent()) {
                            DisplayAnnotation displayAnnotation =
                                    getServiceAnnotation(objectFieldNode.metadata().get().annotations(), filePath);
                            serviceId = displayAnnotation.getId() != null ? displayAnnotation.getId() :
                                    Integer.toString(objectFieldNode.hashCode());
                        }
                        Connection dependency = new Connection(serviceId,
                                getClientModuleName(referredClassSymbol),
                                getSourceLocation(filePath, objectFieldNode.lineRange()), Collections.emptyList());
                        dependencies.add(dependency);
                    }
                }
            }
        }
    }

    @Override
    public void visit(ConstantDeclarationNode constantDeclarationNode) {

    }

    private boolean hasInvocationReferences(ObjectFieldNode clientDeclarationNode) {
        Optional<Symbol> objFieldNodeSymbol = semanticModel.symbol(clientDeclarationNode);
        if (objFieldNodeSymbol.isEmpty()) {
            return false;
        }
        List<LineRange> objFieldNodeRefs = semanticModel.references(objFieldNodeSymbol.get())
                .stream().map(Location::lineRange).collect(Collectors.toList());
        for (LineRange lineRange : objFieldNodeRefs) {
            Node referredNode = findNode(syntaxTree, lineRange);
            while (!referredNode.kind().equals(SyntaxKind.SERVICE_DECLARATION) &&
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
