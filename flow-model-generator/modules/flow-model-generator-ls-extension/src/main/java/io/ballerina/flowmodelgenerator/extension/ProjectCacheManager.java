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

package io.ballerina.flowmodelgenerator.extension;

import io.ballerina.tools.text.TextDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Manages the cache of the temporarily copied project directory.
 *
 * @since 1.4.0
 */
public class ProjectCacheManager {

    private final Path sourceDir;
    private final Path filePath;
    private Path destinationProjectPath;
    private Path destinationPath;

    public ProjectCacheManager(Path sourceDir, Path filePath) {
        this.sourceDir = sourceDir;
        this.filePath = filePath;
    }

    public void createTempDirectory() throws IOException {
        // Create a temporary directory
        Path tempDir = Files.createTempDirectory("project-cache");
        Path tempDesintaitonPath = tempDir.resolve(sourceDir.getFileName());
        destinationProjectPath = tempDesintaitonPath;

        // Copy contents from sourceDir to destinationDir
        if (Files.isDirectory(sourceDir)) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.forEach(source -> {
                    try {
                        Files.copy(source, tempDesintaitonPath.resolve(sourceDir.relativize(source)),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy project directory to cache", e);
                    }
                });
            }
            return;
        }
        Files.copy(sourceDir, tempDesintaitonPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteCache() throws IOException {
        if (Files.isDirectory(destinationProjectPath)) {
            try (Stream<Path> paths = Files.walk(destinationProjectPath)) {
                paths.sorted(Comparator.reverseOrder()).forEach(source -> {
                    try {
                        Files.delete(source);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete destination directory", e);
                    }
                });
            }
            return;
        }
        Files.delete(destinationProjectPath);
    }

    public void writeContent(TextDocument textDocument) throws IOException {
        if (destinationProjectPath == null) {
            throw new RuntimeException("Destination directory is not created");
        }
        Files.writeString(getDestination(), new String(textDocument.toCharArray()), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path getDestination() {
        if (destinationProjectPath == null) {
            throw new RuntimeException("Destination directory is not created");
        }
        if (destinationPath == null) {
            destinationPath = destinationProjectPath.resolve(sourceDir.relativize(sourceDir.resolve(filePath)));
        }
        return destinationPath;
    }
}
