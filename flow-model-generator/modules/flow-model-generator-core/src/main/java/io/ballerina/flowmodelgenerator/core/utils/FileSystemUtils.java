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

package io.ballerina.flowmodelgenerator.core.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectException;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.MessageType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileSystemUtils {

    private static final List<Path> CREATED_FILES = new ArrayList<>();

    public static Document getDocument(WorkspaceManager workspaceManager, Path filePath) {
        Document document;
        try {
            document = workspaceManager.document(filePath).orElseThrow();
        } catch (ProjectException e) {
            // Create a new file as it does not exist
            try {
                Files.createFile(filePath);
                CREATED_FILES.add(filePath);
                FileEvent fileEvent = new FileEvent(filePath.toUri().toString(), FileChangeType.Created);
                workspaceManager.didChangeWatched(filePath, fileEvent);
                document = workspaceManager.document(filePath).orElseThrow();
            } catch (IOException | WorkspaceDocumentException fileCreationException) {
                throw new RuntimeException("Error occurred while creating the file: " + filePath,
                        fileCreationException);
            }
        }
        return document;
    }

    public static SemanticModel getSemanticModel(WorkspaceManager workspaceManager, Path filePath) {
        Optional<SemanticModel> optionalSemanticModel = workspaceManager.semanticModel(filePath);
        if (optionalSemanticModel.isPresent()) {
            return optionalSemanticModel.get();
        }

        // Obtain the default semantic model if not exists
        Project project = workspaceManager.project(filePath).orElseThrow();
        return project.currentPackage().getDefaultModule().getCompilation().getSemanticModel();
    }

    public static void deleteCreatedFiles() {
        CREATED_FILES.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while deleting the file: " + path, e);
            }
        });
    }
}
