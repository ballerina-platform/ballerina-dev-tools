/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.artifactsgenerator;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generator class responsible for creating artifacts from a Ballerina syntax tree. This class analyzes the module
 * members in a syntax tree to extract artifact information.
 *
 * @since 2.3.0
 */
public class EventGenerator {

    public static List<Artifact> artifactChanges(SyntaxTree syntaxTree, SemanticModel semanticModel) {
        if (!syntaxTree.containsModulePart()) {
            return List.of();
        }
        ModulePartNode rootNode = syntaxTree.rootNode();
        NodeList<ModuleMemberDeclarationNode> members = rootNode.members();
        ModuleNodeTransformer moduleNodeTransformer = new ModuleNodeTransformer(semanticModel);
        return members.stream().parallel()
                .map(member -> member.apply(moduleNodeTransformer))
                .flatMap(Optional::stream)
                .toList();
    }

    public static List<Artifact> artifacts(Project project) {
        List<Artifact> artifacts = new ArrayList<>();
        Module defaultModule = project.currentPackage().getDefaultModule();
        defaultModule.documentIds().stream().parallel().forEach(documentId -> {
            SyntaxTree syntaxTree = defaultModule.document(documentId).syntaxTree();
            artifacts.addAll(artifactChanges(syntaxTree,
                    project.currentPackage().getCompilation().getSemanticModel(defaultModule.moduleId())));
        });
        return artifacts;
    }
}
