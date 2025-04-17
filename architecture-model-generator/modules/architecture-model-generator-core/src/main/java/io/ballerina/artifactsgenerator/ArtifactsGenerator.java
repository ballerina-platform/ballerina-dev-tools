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
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.modelgenerator.commons.PackageUtil;
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
import java.util.concurrent.ConcurrentMap;

/**
 * Generator class responsible for creating artifacts from a Ballerina syntax tree. This class analyzes the module
 * members in a syntax tree to extract artifact information.
 *
 * @since 2.3.0
 */
public class ArtifactsGenerator {

    private static final String ADDITIONS = "additions";
    private static final String UPDATES = "updates";
    private static final String DELETIONS = "deletions";

    public static Map<String, Map<String, Map<String, Artifact>>> artifactChanges(String projectPath,
                                                                                  SyntaxTree syntaxTree,
                                                                                  SemanticModel semanticModel) {
        if (!syntaxTree.containsModulePart()) {
            return Map.of();
        }

        Map<String, List<String>> prevIdMap = new HashMap<>(
                ArtifactsCache.getInstance().getArtifactIds(projectPath, syntaxTree.filePath()));
        Map<String, List<String>> newIdMap = new HashMap<>();

        Map<String, Map<String, Map<String, Artifact>>> categoryMap = new HashMap<>();
        ModulePartNode rootNode = syntaxTree.rootNode();
        ModuleNodeTransformer moduleNodeTransformer = new ModuleNodeTransformer(semanticModel);
        rootNode.members().stream()
                .map(member -> member.apply(moduleNodeTransformer))
                .flatMap(Optional::stream)
                .forEach(artifact -> {
                    String category = Artifact.getCategory(artifact.type());
                    String artifactId = artifact.id();

                    // Determine if this is an update or an addition
                    List<String> prevIds = prevIdMap.get(category);
                    String eventType;
                    if (prevIds != null && prevIds.remove(artifactId)) {
                        eventType = UPDATES;
                    } else {
                        eventType = ADDITIONS;
                    }

                    // Update the new artifact
                    categoryMap.computeIfAbsent(category, k -> new HashMap<>())
                            .computeIfAbsent(eventType, k -> new HashMap<>())
                            .put(artifactId, artifact);
                    newIdMap.computeIfAbsent(category, k -> new ArrayList<>()).add(artifactId);
                });

        // Process remaining items in prevIdMap as deletions
        prevIdMap.forEach((category, remainingIds) -> {
            if (!remainingIds.isEmpty()) {
                remainingIds.forEach(id -> categoryMap
                        .computeIfAbsent(category, k -> new HashMap<>())
                        .computeIfAbsent(DELETIONS, k -> new HashMap<>())
                        .put(id, Artifact.emptyArtifact(id)));
            }
        });

        // Update the artifacts cache
        ArtifactsCache.getInstance().updateArtifactIds(projectPath, syntaxTree.filePath(), newIdMap);
        return categoryMap;
    }

    public static Map<String, Map<String, Artifact>> artifacts(Project project) {
        Package currentPackage = project.currentPackage();
        Module defaultModule = currentPackage.getDefaultModule();
        SemanticModel semanticModel =
                PackageUtil.getCompilation(currentPackage).getSemanticModel(defaultModule.moduleId());

        Map<String, Map<String, Artifact>> artifactMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, Map<String, List<String>>> documentMap = new ConcurrentHashMap<>();
        defaultModule.documentIds().stream().parallel().forEach(documentId -> {
            Document document = defaultModule.document(documentId);
            Map<String, List<String>> idMap = new HashMap<>();
            SyntaxTree syntaxTree = document.syntaxTree();
            ModulePartNode rootNode = syntaxTree.rootNode();
            ModuleNodeTransformer moduleNodeTransformer = new ModuleNodeTransformer(semanticModel);
            rootNode.members().stream()
                    .map(member -> member.apply(moduleNodeTransformer))
                    .flatMap(Optional::stream)
                    .forEach(artifact -> {
                        String category = Artifact.getCategory(artifact.type());
                        String artifactId = artifact.id();
                        artifactMap.computeIfAbsent(category, k -> new HashMap<>()).put(artifactId, artifact);
                        idMap.computeIfAbsent(category, k -> new ArrayList<>()).add(artifactId);
                    });
            documentMap.put(document.name(), idMap);
        });

        ArtifactsCache.getInstance().initializeProject(project.sourceRoot().toString(), documentMap);
        return artifactMap;
    }
}
