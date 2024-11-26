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
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Project;
import io.ballerina.sequencemodelgenerator.core.model.Participant;
import io.ballerina.tools.diagnostics.Location;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the participants in the sequence diagram.
 *
 * @since 2.0.0
 */
public class ParticipantManager {

    private static ParticipantManager instance = null;
    private final Map<String, String> cache;
    private final List<Participant> participants;
    private final SemanticModel semanticModel;
    private final Project project;
    private final String packageName;

    private ParticipantManager(SemanticModel semanticModel, Project project) {
        this.cache = new HashMap<>();
        this.participants = new ArrayList<>();
        this.semanticModel = semanticModel;
        this.project = project;
        this.packageName = project.currentPackage().packageName().toString();
    }

    /**
     * Initializes the participant manager. This method should be called before calling any other method.
     *
     * @param semanticModel semantic model of the sequence diagram
     * @param project       project of the sequence diagram
     */
    public static void initialize(SemanticModel semanticModel, Project project) {
        instance = new ParticipantManager(semanticModel, project);
    }

    /**
     * Returns the participant manager instance.
     *
     * @return participant manager instance
     */
    public static ParticipantManager getInstance() {
        return instance;
    }

    /**
     * Returns the participant ID of the given participant name. Generates the participant if not found.
     *
     * @param name participant name
     * @return participant ID
     */
    public String getParticipantId(Node name) {
        String participantId = cache.get(name.toString());
        if (participantId != null) {
            return participantId;
        }
        try {
            Symbol symbol = semanticModel.symbol(name).orElseThrow();
            Location location = symbol.getLocation().orElseThrow();
            String fileName = location.lineRange().fileName();
            String moduleName = CommonUtil.getModuleName(symbol).orElseThrow();
            Path filePath = CommonUtil.getFilePath(project, fileName,
                    moduleName.equals(packageName) ? null : moduleName);
            SyntaxTree syntaxTree = CommonUtil.getSyntaxTree(project, filePath);
            SemanticModel moduleSemanticModel = CommonUtil.getSemanticModel(project, filePath);
            NonTerminalNode participantNode = CommonUtil.getNode(syntaxTree, location.textRange());
            return generateParticipant(moduleSemanticModel, participantNode, moduleName);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Generates the participant node.
     *
     * @param participantNode participant node
     * @param moduleName      module name of the participant
     * @return participant ID
     */
    public String generateParticipant(SemanticModel moduleSemanticModel, Node participantNode, String moduleName) {
        ParticipantAnalyzer participantAnalyzer = new ParticipantAnalyzer(moduleSemanticModel, moduleName);
        participantNode.accept(participantAnalyzer);
        Participant participant = participantAnalyzer.getParticipant();
        participants.add(participant);
        String participantId = participant.id();
        cache.put(participant.name(), participantId);
        return participantId;
    }

    /**
     * Returns the participants in the sequence diagram.
     *
     * @return participants in the sequence diagram
     */
    public List<Participant> getParticipants() {
        return participants;
    }
}
