package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.*;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;
import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Optional;

import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;

public class ModuleLevelConnectorVisitor extends NodeVisitor {
    private final SemanticModel semanticModel;
    private final List<Participant> participants;

    public List<Participant> getParticipants() {
        return participants;
    }

    public ModuleLevelConnectorVisitor(SemanticModel semanticModel, List<Participant> participants) {
        this.semanticModel = semanticModel;
        this.participants = participants;
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        if (moduleVariableDeclarationNode.initializer().isPresent()) {
            LineRange lineRange =  moduleVariableDeclarationNode.initializer().get().lineRange();
            Optional<TypeSymbol> typeSymbol = this.semanticModel.typeOf(lineRange);
            if (typeSymbol.isPresent()) {
                TypeSymbol rawType = getRawType(typeSymbol.get());
                if (rawType.typeKind() == TypeDescKind.OBJECT) {
                    ObjectTypeSymbol objectTypeSymbol = (ObjectTypeSymbol) rawType;
                    boolean isEndpoint = objectTypeSymbol.qualifiers().contains(Qualifier.CLIENT);
                    String endpointName = getEndpointName(moduleVariableDeclarationNode);

                    if (isEndpoint && endpointName != null && objectTypeSymbol.getModule().isPresent()) {
                        String endpointID = generateEndpointID(objectTypeSymbol, endpointName);
                        String clientPkgName = ModelGeneratorUtils.generateModuleIDFromSymbol(objectTypeSymbol);
                        if (isEndpointNotPresentParticipants(endpointID) && clientPkgName != null) {
                            Participant participant = new Participant(endpointID, endpointName,
                                    ParticipantKind.ENDPOINT, clientPkgName, objectTypeSymbol.signature().trim(),
                                    objectTypeSymbol.getLocation().isPresent() ? objectTypeSymbol.getLocation().get().lineRange() : null, false);
                            this.participants.add(participant);
                        }
                    }
                }
            }

        }
    }

    private String generateEndpointID(ObjectTypeSymbol objectTypeSymbol, String endpointName) {
        if (objectTypeSymbol.getModule().isPresent()) {
            String clientPkgName = objectTypeSymbol.getModule().get().id().toString().trim().replace(":", "_");
            return clientPkgName + "_" + objectTypeSymbol.signature().trim() + "_" + endpointName;
        }
        return null;
    }

    private String getEndpointName(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        String endpointName = null;
        if (moduleVariableDeclarationNode.typedBindingPattern().bindingPattern().kind() ==
                SyntaxKind.CAPTURE_BINDING_PATTERN) {
            endpointName = ((CaptureBindingPatternNode) moduleVariableDeclarationNode
                    .typedBindingPattern().bindingPattern()).variableName().text();
        }
        return endpointName;
    }



    private boolean isEndpointNotPresentParticipants(String clientID) {
        for (Participant endpoint : this.participants) {
            if (endpoint.getParticipantKind().equals(ParticipantKind.ENDPOINT)) {
                if (endpoint.getId().equals(clientID.trim())) {
                    return false; // Return false when the conditions are met
                }
            }
        }
        return true; // Return true if no match is found
    }
}
