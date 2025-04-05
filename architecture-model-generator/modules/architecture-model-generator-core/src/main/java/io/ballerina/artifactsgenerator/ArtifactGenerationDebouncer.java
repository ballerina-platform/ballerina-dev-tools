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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Debouncer for artifact generation to ensure artifacts are only generated after a specified delay has passed since the
 * last request for the same file.
 *
 * @since 2.3.0
 */
public class ArtifactGenerationDebouncer {

    // Default delay in milliseconds
    private static final long DEFAULT_DELAY = 500;
    // Time unit for the delay
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    // Map to hold scheduled tasks
    private final ConcurrentHashMap<String, ScheduledTaskHolder> delayedMap;

    // Single-thread scheduler to debounce tasks.
    private final ScheduledExecutorService scheduler;

    private ArtifactGenerationDebouncer() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        delayedMap = new ConcurrentHashMap<>();
    }

    /**
     * Debounce the given artifact generation task by scheduling it to execute after the default delay. Any previously
     * scheduled task with the same key is cancelled.
     *
     * @param key  The key to identify the task (usually a file name)
     * @param task The task to execute
     */
    public void debounce(String key, Runnable task) {
        debounce(key, task, DEFAULT_DELAY);
    }

    /**
     * Debounce the given artifact generation task by scheduling it to execute after the provided delay. Any previously
     * scheduled task with the same key is cancelled.
     *
     * @param key   The key to identify the task (usually a file name)
     * @param task  The task to execute
     * @param delay The delay in milliseconds
     */
    public void debounce(String key, Runnable task, long delay) {
        CompletableFuture<Void> promise = new CompletableFuture<>();

        // Schedule the task to run after the specified delay.
        Future<?> scheduledFuture = scheduler.schedule(() -> {
            try {
                task.run();
                promise.complete(null);
            } catch (Exception ex) {
                promise.completeExceptionally(ex);
            } finally {
                delayedMap.remove(key);
            }
        }, delay, TIME_UNIT);

        // Replace any existing scheduled task with the new one.
        ScheduledTaskHolder prev = delayedMap.put(key, new ScheduledTaskHolder(promise, scheduledFuture));
        if (prev != null) {
            prev.future.cancel(true);
            prev.promise.completeExceptionally(new CancellationException("Debounced by a new request"));
        }
    }

    public static ArtifactGenerationDebouncer getInstance() {
        return Holder.INSTANCE;
    }

    private static class Holder {

        private static final ArtifactGenerationDebouncer INSTANCE = new ArtifactGenerationDebouncer();
    }

    private record ScheduledTaskHolder(CompletableFuture<Void> promise, Future<?> future) {
    }
}
