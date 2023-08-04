package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WorkerMemberNodeVisitor extends NodeVisitor {
    private List<Participant> participants;
    private final SemanticModel semanticModel;
    private String workerId;


    public WorkerMemberNodeVisitor(SemanticModel semanticModel, String workerId, List<Participant> participants) {
        this.semanticModel = semanticModel;
        this.workerId = workerId;
        this.participants = participants;
    }




    // Adds participants without interactions, endpoints
    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        // this will be handle by the action visitor
//        if (hasInvocationReferences(variableDeclarationNode)) {
//            return;
//        }
        Node fieldTypeName = getReferredNode(variableDeclarationNode.typedBindingPattern().typeDescriptor());
        if (fieldTypeName != null) {
            Optional<Symbol> fieldTypeNameSymbol = semanticModel.symbol(fieldTypeName);
            if (fieldTypeNameSymbol.isPresent()) {
                ClassSymbol referredClassSymbol = getReferredClassSymbol((TypeSymbol) fieldTypeNameSymbol.get());
                if (referredClassSymbol != null) {
                    boolean isClientClass = referredClassSymbol.qualifiers().contains(Qualifier.CLIENT);
                    if (isClientClass) {
                        if (!isParticipantInList(
                                variableDeclarationNode.typedBindingPattern().bindingPattern().toString(),
                                referredClassSymbol.getModule().get().id().toString())) {
                            String clientID = UUID.randomUUID().toString();
                            Participant participant = new Participant(clientID, variableDeclarationNode.typedBindingPattern().bindingPattern().toString(),
                                    ParticipantKind.ENDPOINT,referredClassSymbol.getModule().get().id().toString(),referredClassSymbol.signature());
                            participants.add(participant);

                        }
                    }
                }
            }
        }
    }

    private TypeSymbol getRawType(TypeSymbol typeDescriptor) {
        return typeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE
                ? ((TypeReferenceTypeSymbol) typeDescriptor).typeDescriptor() : typeDescriptor;
    }

    private Node getReferredNode(Node typeName) {
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

    private ClassSymbol getReferredClassSymbol(TypeSymbol symbol) {
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

    private boolean isParticipantInList(String name, String pkgName) {
        for (Participant item : participants) {
            if (item.getName().trim().equals(name.trim()) &&
                    item.getPackageName().trim().equals(pkgName.trim())) {
                return true; // Return true when the conditions are met
            }
        }
        return false; // Return false if no match is found
    }
}
