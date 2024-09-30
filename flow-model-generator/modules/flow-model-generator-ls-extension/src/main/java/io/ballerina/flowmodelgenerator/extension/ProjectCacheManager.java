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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages the cache of the temporarily copied project directory.
 *
 * @since 1.4.0
 */
public class ProjectCacheManager {

    private final Path sourceDir;
    private Path destinationProjectPath;

    public ProjectCacheManager(Path sourceDir) {
        this.sourceDir = sourceDir;
    }

    public void createTempDirectory() throws IOException {
        // Create a temporary directory
        if (destinationProjectPath == null) {
            Path tempDir = Files.createTempDirectory("project-cache");
            destinationProjectPath = tempDir.resolve(sourceDir.getFileName());
        } else {
            Files.createDirectories(destinationProjectPath);
        }

        // Copy contents from sourceDir to destinationDir
        if (Files.isDirectory(sourceDir)) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.forEach(source -> {
                    try {
                        Files.copy(source, destinationProjectPath.resolve(sourceDir.relativize(source)),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy project directory to cache", e);
                    }
                });
            }
            return;
        }
        Files.copy(sourceDir, destinationProjectPath, StandardCopyOption.REPLACE_EXISTING);
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

    public void writeContent(TextDocument textDocument, Path filePath) throws IOException {
        if (destinationProjectPath == null) {
            throw new RuntimeException("Destination directory is not created");
        }
        Files.writeString(getDestination(filePath), new String(textDocument.toCharArray()), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path getDestination(Path filePath) {
        if (destinationProjectPath == null) {
            throw new RuntimeException("Destination directory is not created");
        }
        return destinationProjectPath.resolve(sourceDir.relativize(sourceDir.resolve(filePath)));
    }

    /**
     * The multiton design pattern to handle `ProjectCacheManager` instances mapped by source directory. Ensures that
     * there is only one copy per each project.
     *
     * @since 1.4.0
     */
    public static class InstanceHandler {

        private static final Map<Path, ProjectCacheManager> instances = new ConcurrentHashMap<>();

        private InstanceHandler() {
        }

        public static ProjectCacheManager getInstance(Path sourceDir, Path filePath) {
            return instances.computeIfAbsent(sourceDir, key -> new ProjectCacheManager(sourceDir));
        }

        public static void removeInstance(Path sourceDir) throws IOException {
            ProjectCacheManager manager = instances.remove(sourceDir);
            if (manager != null) {
                manager.deleteCache();
            }
        }
    }
}
