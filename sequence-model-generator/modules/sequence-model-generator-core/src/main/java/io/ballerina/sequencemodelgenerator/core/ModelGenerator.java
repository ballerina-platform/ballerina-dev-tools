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
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

/**
 * Represents the root model generator for sequence diagram.
 *
 * @since 2201.9.0
 */
public class ModelGenerator {

    public Diagram getSequenceDiagramModel(Project project, LineRange lineRange, SemanticModel semanticModel) {
        // Obtain the block representing the diagram
        SyntaxTree syntaxTree = CommonUtil.getSyntaxTree(project, lineRange.fileName(), null);
        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        TextRange textRange = TextRange.from(start, end - start);
        NonTerminalNode rootNode = CommonUtil.getNode(syntaxTree, textRange);
        ParticipantManager.initialize(semanticModel, project);

        // Generate the participant nodes
        String moduleName = semanticModel.symbol(rootNode)
                .flatMap(CommonUtil::getModuleName)
                .orElse(Constants.DEFAULT_MODULE);

        ParticipantManager participantManager = ParticipantManager.getInstance();
        participantManager.generateParticipant(rootNode, moduleName);
        return new Diagram(participantManager.getParticipants(), lineRange);
    }
}
