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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Singleton cache for storing project artifacts. This class provides thread-safe access to artifact IDs mapped by
 * project ID and file URI.
 *
 * @since 2.3.0
 */
public class ArtifactsCache {

    private static ArtifactsCache instance;

    // Map: project_id → document id -> category -> artifact ids
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Map<String, List<String>>>> projectCache;

    // Map: project_id:file_uri → lock
    private final ConcurrentHashMap<String, Lock> locks;

    private ArtifactsCache() {
        projectCache = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the singleton instance of ArtifactsCache.
     */
    public static void initialize() {
        instance = new ArtifactsCache();
    }

    /**
     * Gets the singleton instance of ArtifactsCache.
     *
     * @return The singleton instance
     */
    public static ArtifactsCache getInstance() {
        return instance;
    }

    private Lock getOrCreateLock(String projectId, String fileUri) {
        String lockKey = projectId + ":" + fileUri;
        return locks.computeIfAbsent(lockKey, k -> new ReentrantLock());
    }

    /**
     * Checks if a project exists in the cache.
     *
     * @param projectId The project ID
     * @return true if the project exists, false otherwise
     */
    public boolean hasInitialized(String projectId) {
        return projectCache.containsKey(projectId);
    }

    public void initializeProject(String projectId,
                                  ConcurrentHashMap<String, Map<String, List<String>>> documentMap) {
        projectCache.put(projectId, documentMap);
    }

    /**
     * Gets artifact IDs for a given file URI within a project. This method acquires a lock that must be released by
     * calling updateArtifactIds.
     *
     * @param projectId The project ID
     * @param fileUri   The file URI
     * @return List of artifact IDs, or empty list if not found
     */
    public Map<String, List<String>> getArtifactIds(String projectId, String fileUri) {
        Lock lock = getOrCreateLock(projectId, fileUri);
        lock.lock();

        ConcurrentHashMap<String, Map<String, List<String>>> documentMap = projectCache.get(projectId);
        if (documentMap == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> artifactIds = documentMap.get(fileUri);
        if (artifactIds == null) {
            return Collections.emptyMap();
        }

        return artifactIds;
    }

    /**
     * Adds or updates artifact IDs for a file URI within a project. This method releases the lock acquired by
     * getArtifactIds.
     *
     * @param projectId   The project ID
     * @param fileUri     The file URI
     * @param artifactIds The list of artifact IDs
     */
    public void updateArtifactIds(String projectId, String fileUri, Map<String, List<String>> artifactIds) {
        try {
            // Get or create document map for project
            ConcurrentHashMap<String, Map<String, List<String>>> documentMap =
                    projectCache.computeIfAbsent(projectId, k -> new ConcurrentHashMap<>());
            documentMap.put(fileUri, artifactIds);
        } finally {
            // Release the lock acquired in getArtifactIds
            Lock lock = getOrCreateLock(projectId, fileUri);
            lock.unlock();
        }
    }
}
