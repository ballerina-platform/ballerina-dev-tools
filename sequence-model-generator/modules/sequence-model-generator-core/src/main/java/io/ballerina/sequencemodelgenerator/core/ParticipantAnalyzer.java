/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.sequencemodelgenerator.core.model.SequenceNode;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Objects;

/**
 * Analyzes the participant in the sequence diagram.
 *
 * @since 2.0.0
 */
public class ParticipantAnalyzer extends NodeVisitor {

    private final SemanticModel semanticModel;
    private String name;
    private final String moduleName;
    private Participant.ParticipantKind kind;
    private LineRange location;
    private List<SequenceNode> sequenceNodes;

    public ParticipantAnalyzer(SemanticModel semanticModel, String moduleName) {
        this.semanticModel = semanticModel;
        this.moduleName = moduleName;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        name = functionDefinitionNode.functionName().text();
        kind = Participant.ParticipantKind.FUNCTION;
        location = functionDefinitionNode.location().lineRange();

        ParticipantBodyAnalyzer participantBodyAnalyzer = new ParticipantBodyAnalyzer(semanticModel);
        functionDefinitionNode.functionBody().accept(participantBodyAnalyzer);
        sequenceNodes = participantBodyAnalyzer.getSequenceNodes();
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        kind = Participant.ParticipantKind.ENDPOINT;
        location = moduleVariableDeclarationNode.location().lineRange();
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        kind = Participant.ParticipantKind.ENDPOINT;
        location = variableDeclarationNode.location().lineRange();
    }

    @Override
    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {
        name = captureBindingPatternNode.variableName().text();
        captureBindingPatternNode.parent().parent().accept(this);
    }

    public Participant getParticipant() {
        String id = String.valueOf(Objects.hash(location));
        return new Participant(id, name, kind, moduleName, sequenceNodes, location);
    }
}
