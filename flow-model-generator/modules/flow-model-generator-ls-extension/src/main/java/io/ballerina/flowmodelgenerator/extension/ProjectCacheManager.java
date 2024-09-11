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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public class ProjectCacheManager {

    private final Path sourceDir;
    private final Path filePath;
    private Path destinationDir;

    public ProjectCacheManager(Path sourceDir, Path filePath) {
        this.sourceDir = sourceDir;
        this.filePath = filePath;
    }

    public void createTempDirectoryWithContents() throws IOException {
        // Create a temporary directory
        Path tempDir = Files.createTempDirectory("project-cache");
        destinationDir = tempDir.resolve(sourceDir.getFileName());

        // Copy contents from sourceDir to destinationDir
        if (Files.isDirectory(sourceDir)) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.forEach(source -> {
                    try {
                        Files.copy(source, destinationDir.resolve(sourceDir.relativize(source)),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy project directory to cache", e);
                    }
                });
            }
        } else {
            Files.copy(sourceDir, destinationDir, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void deleteCache() throws IOException {
        if (Files.isDirectory(destinationDir)) {
            try (Stream<Path> paths = Files.walk(destinationDir)) {
                paths.sorted(Comparator.reverseOrder()).forEach(source -> {
                    try {
                        Files.delete(source);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete destination directory", e);
                    }
                });
            }
        } else {
            Files.delete(destinationDir);
        }
    }

    public Path getDestination() {
        return destinationDir.resolve(sourceDir.relativize(sourceDir.resolve(filePath)));
    }
}