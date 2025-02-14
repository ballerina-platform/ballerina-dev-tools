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
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Project;
import io.ballerina.sequencemodelgenerator.core.model.Diagram;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents the model generator for the sequence diagram.
 *
 * @since 2.0.0
 */
public class ModelGenerator {

    /**
     * Generates the sequence diagram model.
     *
     * @param project       project of the diagram
     * @param lineRange     line range of the participant
     * @param semanticModel semantic model of the diagram
     * @return the sequence diagram model
     */
    public static Diagram getSequenceDiagramModel(Project project, LineRange lineRange, SemanticModel semanticModel) {
        // Obtain the node representing the root participant
        Path filePath = CommonUtil.getFilePath(project, lineRange.fileName(), null);
        SyntaxTree syntaxTree = CommonUtil.getSyntaxTree(project, filePath);
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        TextRange textRange = TextRange.from(start, end - start);
        NonTerminalNode rootNode = CommonUtil.getNode(syntaxTree, textRange);

        // Generate the participant nodes
        String moduleName = semanticModel.symbol(rootNode)
                .flatMap(CommonUtil::getModuleName)
                .orElse(Constants.DEFAULT_MODULE);
        ParticipantManager.initialize(semanticModel, project);
        ParticipantManager participantManager = ParticipantManager.getInstance();
        participantManager.generateParticipant(semanticModel, rootNode, moduleName);
        List<Participant> participants = participantManager.getParticipants();

        return new Diagram(participants, lineRange);
    }
}
