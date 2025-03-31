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
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Generator class responsible for creating artifacts from a Ballerina syntax tree. This class analyzes the module
 * members in a syntax tree to extract artifact information.
 *
 * @since 2.3.0
 */
public class EventGenerator {

    public static Map<String, Map<String, Artifact>> artifactChanges(String filePath,
                                                                     SyntaxTree syntaxTree,
                                                                     SemanticModel semanticModel) {
        if (!syntaxTree.containsModulePart()) {
            return Map.of();
        }
        Map<String, Map<String, Artifact>> categoryMap = new ConcurrentHashMap<>();
        findArtifacts(categoryMap, syntaxTree, semanticModel);
        return toUnmodifableMap(categoryMap);
    }

    public static Map<String, Map<String, Artifact>> artifacts(Project project) {
        Package currentPackage = project.currentPackage();
        Module defaultModule = currentPackage.getDefaultModule();
        SemanticModel semanticModel = currentPackage.getCompilation().getSemanticModel(defaultModule.moduleId());

        Map<String, Map<String, Artifact>> categoryMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, CopyOnWriteArrayList<String>> documentMap = new ConcurrentHashMap<>();
        defaultModule.documentIds().stream().parallel().forEach(documentId -> {
            Document document = defaultModule.document(documentId);
            List<String> artifacts = findArtifacts(categoryMap, document.syntaxTree(), semanticModel);
            documentMap.put(document.name(), new CopyOnWriteArrayList<>(artifacts));
        });

        ArtifactsCache.getInstance().initializeProject(project.sourceRoot().toString(), documentMap);
        return toUnmodifableMap(categoryMap);
    }

    private static List<String> findArtifacts(Map<String, Map<String, Artifact>> categoryMap,
                                              SyntaxTree syntaxTree, SemanticModel semanticModel) {
        List<String> artifactIds = new ArrayList<>();
        ModulePartNode rootNode = syntaxTree.rootNode();
        NodeList<ModuleMemberDeclarationNode> members = rootNode.members();
        ModuleNodeTransformer moduleNodeTransformer = new ModuleNodeTransformer(semanticModel);
        members.stream().parallel()
                .map(member -> member.apply(moduleNodeTransformer))
                .flatMap(Optional::stream)
                .forEach(artifact -> {
                    String category = artifact.type().getCategory();
                    String artifactId = artifact.id();
                    categoryMap.computeIfAbsent(category, k -> new HashMap<>()).put(artifactId, artifact);
                    artifactIds.add(artifactId);
                });
        return artifactIds;
    }

    private static Map<String, Map<String, Artifact>> toUnmodifableMap(Map<String, Map<String, Artifact>> categoryMap) {
        return categoryMap.entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> Map.copyOf(e.getValue())));
    }
}
