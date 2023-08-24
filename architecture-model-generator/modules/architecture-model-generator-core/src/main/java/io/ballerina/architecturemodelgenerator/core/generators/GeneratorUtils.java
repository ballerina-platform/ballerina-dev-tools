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

package io.ballerina.architecturemodelgenerator.core.generators;

import io.ballerina.architecturemodelgenerator.core.model.SourceLocation;
import io.ballerina.architecturemodelgenerator.core.model.common.DisplayAnnotation;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Annotatable;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ParenthesisedTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.projects.Package;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static io.ballerina.architecturemodelgenerator.core.Constants.CLIENT;
import static io.ballerina.architecturemodelgenerator.core.Constants.DISPLAY_ANNOTATION;
import static io.ballerina.architecturemodelgenerator.core.Constants.ID;
import static io.ballerina.architecturemodelgenerator.core.Constants.LABEL;

/**
 * Provide utils functions for component model generating.
 *
 * @since 2201.3.1
 */
public class GeneratorUtils {

    public static SourceLocation getSourceLocation(String filePath, LineRange lineRange) {

        SourceLocation.LinePosition startPosition = SourceLocation.LinePosition.from(
                lineRange.startLine().line(), lineRange.startLine().offset());
        SourceLocation.LinePosition endLinePosition = SourceLocation.LinePosition.from(
                lineRange.endLine().line(), lineRange.endLine().offset()
        );
        return SourceLocation.from(filePath, startPosition, endLinePosition);

    }

    public static DisplayAnnotation getServiceAnnotation(NodeList<AnnotationNode> annotationNodes, String filePath) {

        String id = "";
        String label = "";
        SourceLocation elementLocation = null;
        for (AnnotationNode annotationNode : annotationNodes) {
            String annotationName = annotationNode.annotReference().toString().trim();
            if (!(annotationName.equals(DISPLAY_ANNOTATION) && annotationNode.annotValue().isPresent())) {
                continue;
            }
            SeparatedNodeList<MappingFieldNode> fields = annotationNode.annotValue().get().fields();
            elementLocation = getSourceLocation(filePath, annotationNode.lineRange());
            for (MappingFieldNode mappingFieldNode : fields) {
                if (mappingFieldNode.kind() != SyntaxKind.SPECIFIC_FIELD) {
                    continue;
                }
                SpecificFieldNode specificFieldNode = (SpecificFieldNode) mappingFieldNode;
                String name = specificFieldNode.fieldName().toString().trim();
                if (specificFieldNode.valueExpr().isEmpty()) {
                    continue;
                }
                ExpressionNode expressionNode = specificFieldNode.valueExpr().get();
                String expressionNodeStr = expressionNode.toString().trim();
                String annotation = expressionNodeStr.replace("\"", "");
                if (name.equals(ID)) {
                    id = annotation;
                } else if (name.equals(LABEL)) {
                    label = annotation;
                }
            }
            break;
        }

        return new DisplayAnnotation(id, label, elementLocation, Collections.emptyList());
    }

    public static DisplayAnnotation getServiceAnnotation(Annotatable annotableSymbol, String filePath) {

        String id = null;
        String label = "";
        SourceLocation elementLocation = null;

        List<AnnotationSymbol> annotSymbols = annotableSymbol.annotations();
        List<AnnotationAttachmentSymbol> annotAttachmentSymbols = annotableSymbol.annotAttachments();
        if (annotSymbols.size() == annotAttachmentSymbols.size()) {
            for (int i = 0; i < annotSymbols.size(); i++) {
                AnnotationSymbol annotSymbol = annotSymbols.get(i);
                AnnotationAttachmentSymbol annotAttachmentSymbol = annotAttachmentSymbols.get(i);
                String annotName = annotSymbol.getName().orElse("");
                elementLocation = annotAttachmentSymbol.getLocation().isPresent() ?
                        getSourceLocation(filePath, annotAttachmentSymbol.getLocation().get().lineRange()) : null;
                if (!annotName.equals(DISPLAY_ANNOTATION) || annotAttachmentSymbol.attachmentValue().isEmpty() ||
                        !(annotAttachmentSymbol.attachmentValue().get().value() instanceof LinkedHashMap) ||
                        !annotAttachmentSymbol.isConstAnnotation()) {
                    continue;
                }
                LinkedHashMap attachmentValue = (LinkedHashMap) annotAttachmentSymbol.attachmentValue().get().value();
                if (attachmentValue.containsKey(ID)) {
                    id = ((ConstantValue) attachmentValue.get(ID)).value().toString();
                }
                if (attachmentValue.containsKey(LABEL)) {
                    label = ((ConstantValue) attachmentValue.get(LABEL)).value().toString();
                }
                break;
            }
        }

        return new DisplayAnnotation(id, label, elementLocation, Collections.emptyList());
    }

    public static String getClientModuleName(Node clientNode, SemanticModel semanticModel) {

        String clientModuleName = null;
        Optional<TypeSymbol> clientTypeSymbol = semanticModel.typeOf(clientNode);
        if (clientTypeSymbol.isPresent()) {
            clientModuleName = clientTypeSymbol.get().signature().trim().replace(CLIENT, "");
        }

        return clientModuleName;
    }

    public static String getClientModuleName(TypeSymbol typeSymbol) {
        String clientModuleName = typeSymbol.signature().trim().replace(CLIENT, "");
        if (typeSymbol.getModule().isPresent()) {
            clientModuleName = typeSymbol.getModule().get().id().toString();;
        }
        return clientModuleName;
    }

    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        if (lineRange == null) {
            return null;
        }
        try {
            TextDocument textDocument = syntaxTree.textDocument();
            int start = textDocument.textPositionFrom(lineRange.startLine());
            int end = textDocument.textPositionFrom(lineRange.endLine());
            return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Type extraction related methods
    public static List<String> getReferencedType(TypeSymbol typeSymbol, Package currentPackage) {
        List<String> paramTypes = new LinkedList<>();
        TypeDescKind typeDescKind = typeSymbol.typeKind();
        switch (typeDescKind) {
            case TYPE_REFERENCE:
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                paramTypes.add(getReferenceEntityName(typeReferenceTypeSymbol, currentPackage).trim());
                break;
            case UNION:
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                List<TypeSymbol> memberTypeDescriptors = unionTypeSymbol.memberTypeDescriptors();
                for (TypeSymbol memberTypeDescriptor : memberTypeDescriptors) {
                    paramTypes.addAll(getReferencedType(memberTypeDescriptor, currentPackage));
                }
                break;
            case ARRAY:
                ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) typeSymbol;
                if (arrayTypeSymbol.memberTypeDescriptor().typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                    paramTypes.add(getReferenceEntityName(
                            (TypeReferenceTypeSymbol) arrayTypeSymbol.memberTypeDescriptor(), currentPackage).trim());
                } else {
                    paramTypes.add(arrayTypeSymbol.signature().trim());
                }
                break;
            case NIL:
                paramTypes.add("null");
                break;
            default:
                paramTypes.add(typeDescKind.getName());
        }
        return paramTypes;
    }

    private static String getReferenceEntityName(TypeReferenceTypeSymbol typeReferenceTypeSymbol,
                                                 Package currentPackage) {
        String currentPackageName = String.format("%s/%s:%s", currentPackage.packageOrg().value(),
                currentPackage.packageName().value(), currentPackage.packageVersion().value());
        String referenceType = typeReferenceTypeSymbol.signature();
        if (typeReferenceTypeSymbol.getModule().isPresent() &&
                !referenceType.split(":")[0].equals(currentPackageName.split(":")[0])) {
            String orgName = typeReferenceTypeSymbol.getModule().get().id().orgName();
            String packageName = typeReferenceTypeSymbol.getModule().get().id().packageName();
            String modulePrefix = typeReferenceTypeSymbol.getModule().get().id().modulePrefix();
            String recordName = typeReferenceTypeSymbol.getName().get();
            String version = typeReferenceTypeSymbol.getModule().get().id().version();
            referenceType = String.format("%s/%s:%s:%s:%s", orgName, packageName, modulePrefix, version, recordName);
        }
        return referenceType;
    }

    // Dependency related methods
    public static Node getReferredNode(Node typeName) {
        Node qualifiedNameRefNode = null;
        if (typeName.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE) ||
                typeName.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE)) {
            qualifiedNameRefNode = typeName;
        } else if (typeName instanceof UnionTypeDescriptorNode) {
            Node leftTypeDescNode = getReferredNode(((UnionTypeDescriptorNode) typeName).leftTypeDesc());
            Node rightTypeDescNode = getReferredNode(((UnionTypeDescriptorNode) typeName).rightTypeDesc());
            if (leftTypeDescNode != null && (leftTypeDescNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE) ||
                    leftTypeDescNode.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE))) {
                qualifiedNameRefNode = leftTypeDescNode;
            }
            if (rightTypeDescNode != null && (rightTypeDescNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE) ||
                    rightTypeDescNode.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE))) {
                qualifiedNameRefNode = rightTypeDescNode;
            }
        } else if (typeName instanceof ParenthesisedTypeDescriptorNode) {
            Node typeDescNode = getReferredNode(((ParenthesisedTypeDescriptorNode) typeName).typedesc());
            if (typeDescNode != null && (typeDescNode.kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE) ||
                    typeDescNode.kind().equals(SyntaxKind.SIMPLE_NAME_REFERENCE))) {
                qualifiedNameRefNode = typeDescNode;
            }
        }
        return qualifiedNameRefNode;
    }

    public static ClassSymbol getReferredClassSymbol(TypeSymbol symbol) {
        ClassSymbol classSymbol = null;
        if (symbol.kind().equals(SymbolKind.CLASS)) {
            classSymbol = (ClassSymbol) symbol;
        } else if (symbol.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
            TypeReferenceTypeSymbol typeRefTypeSymbol = (TypeReferenceTypeSymbol) symbol;
            TypeSymbol typeDescTypeSymbol = typeRefTypeSymbol.typeDescriptor();
            classSymbol = getReferredClassSymbol(typeDescTypeSymbol);
        }
        return classSymbol;
    }
}
