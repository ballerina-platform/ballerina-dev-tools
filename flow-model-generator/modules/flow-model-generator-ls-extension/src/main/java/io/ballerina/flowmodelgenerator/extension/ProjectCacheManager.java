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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Manages the cache of the temporarily copied project directory.
 *
 * @since 1.4.0
 */
class ProjectCacheManager {

    private final Path sourceDir;
    private final Path tempDir;

    public ProjectCacheManager(Path sourceDir) {
        this.sourceDir = sourceDir;
        try {
            Path tempDirPath = Files.createTempDirectory("project-cache");
            this.tempDir = tempDirPath.resolve(sourceDir.getFileName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void copyContent() throws IOException {
        // Copy contents from sourceDir to destinationDir
        if (Files.isDirectory(sourceDir)) {
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                paths.forEach(source -> {
                    try {
                        Files.copy(source, tempDir.resolve(sourceDir.relativize(source)),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy project directory to cache", e);
                    }
                });
            }
            return;
        }
        Files.copy(sourceDir, tempDir, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteContent() throws IOException {
        if (Files.isDirectory(tempDir)) {
            try (Stream<Path> paths = Files.walk(tempDir)) {
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
        Files.delete(tempDir);
    }

    public void writeContent(TextDocument textDocument, Path filePath) throws IOException {
        if (tempDir == null) {
            throw new RuntimeException("Destination directory is not created");
        }
        Files.writeString(getDestination(filePath), new String(textDocument.toCharArray()), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public Path getDestination(Path filePath) {
        if (tempDir == null) {
            throw new RuntimeException("Destination directory is not created");
        }
        return tempDir.resolve(sourceDir.relativize(sourceDir.resolve(filePath)));
    }

    /**
     * The multiton design pattern to handle `ProjectCacheManager` instances mapped by source directory. Ensures that
     * there is only one copy per each project.
     *
     * @since 1.4.0
     */

    static class InstanceHandler {

        private static final Map<Path, ProjectCacheManager> instances = new ConcurrentHashMap<>();
        private static final Map<Path, Lock> locks = new ConcurrentHashMap<>();

        private InstanceHandler() {
        }

        public static ProjectCacheManager getInstance(Path sourceDir) {
            Lock lock = locks.computeIfAbsent(sourceDir, key -> new ReentrantLock());
            lock.lock();
            return instances.computeIfAbsent(sourceDir, key -> new ProjectCacheManager(sourceDir));
        }

        public static void release(Path sourceDir) {
            Lock lock = locks.get(sourceDir);
            if (lock != null) {
                lock.unlock();
                locks.remove(sourceDir);
            }
        }
    }
}
