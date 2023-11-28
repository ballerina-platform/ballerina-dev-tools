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

package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.ParticipantKind;
import io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Optional;

import static io.ballerina.sequencemodelgenerator.core.utils.ModelGeneratorUtils.getRawType;

/**
 * Visitor to identify the module level connectors without any interaction.
 *
 * @since 2201.8.0
 */
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
            LineRange lineRange = moduleVariableDeclarationNode.initializer().get().lineRange();
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
                                    objectTypeSymbol.getLocation().isPresent() ?
                                            objectTypeSymbol.getLocation().get().lineRange() :
                                            null, false);
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
