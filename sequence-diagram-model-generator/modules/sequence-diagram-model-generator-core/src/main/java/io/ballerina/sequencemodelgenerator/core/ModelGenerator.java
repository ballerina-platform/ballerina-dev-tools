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

package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.sequencemodelgenerator.core.exception.SequenceModelGenerationException;
import io.ballerina.sequencemodelgenerator.core.model.SequenceModel;
import io.ballerina.sequencemodelgenerator.core.visitors.ModuleLevelConnectorVisitor;
import io.ballerina.sequencemodelgenerator.core.visitors.RootNodeVisitor;
import io.ballerina.sequencemodelgenerator.core.visitors.VisitorContext;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;

import static io.ballerina.sequencemodelgenerator.core.Constants.INVALID_NODE_MSG;
import static io.ballerina.sequencemodelgenerator.core.utils.CommonUtils.findNode;

/**
 * Represents the root model generator for sequence diagram.
 *
 * @since 2201.8.0
 */
public class ModelGenerator {
    public SequenceModel getSequenceDiagramModel(Project project, LineRange position, SemanticModel semanticModel)
            throws SequenceModelGenerationException {
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            Path filePath = Path.of(position.fileName());
            docId = project.documentId(filePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            Module currentModule = packageName.getDefaultModule();
            docId = currentModule.documentIds().iterator().next();
            doc = currentModule.document(docId);
        }

        SyntaxTree syntaxTree = doc.syntaxTree();
        NonTerminalNode node = findNode(syntaxTree, position);
        if (node.isMissing()) {
            throw new SequenceModelGenerationException(INVALID_NODE_MSG);
        }

        VisitorContext visitorContext = new VisitorContext();
        RootNodeVisitor workerNodeVisitor = new RootNodeVisitor(semanticModel, packageName, visitorContext);
        node.accept(workerNodeVisitor);
        if (workerNodeVisitor.getModelGenerationException() != null) {
            throw workerNodeVisitor.getModelGenerationException();
        }
        // get the module level connectors without interactions and update the participant list
        ModuleLevelConnectorVisitor moduleLevelConnectorVisitor = new ModuleLevelConnectorVisitor(semanticModel,
                workerNodeVisitor.getVisitorContext().getParticipants());
        syntaxTree.rootNode().accept(moduleLevelConnectorVisitor);
        return new SequenceModel(moduleLevelConnectorVisitor.getParticipants(), position);
    }
}
