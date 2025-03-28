package io.ballerina.artifactsgenerator;

/**
 * Represents an artifact event with its type and the artifact itself.
 *
 * @param eventType The type of event (INSERT, UPDATE, DELETE)
 * @param artifact  The artifact object
 */
public record PublishArtifactEvent(EventType eventType, Object artifact) {

    /**
     * Enum representing the possible event types for artifacts.
     */
    public enum EventType {
        INSERT,
        UPDATE,
        DELETE
    }
}