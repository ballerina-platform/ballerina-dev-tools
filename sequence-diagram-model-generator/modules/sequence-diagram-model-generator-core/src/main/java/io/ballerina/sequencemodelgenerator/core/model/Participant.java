package io.ballerina.sequencemodelgenerator.core.model;

public class Participant {
    private String id;
    private String name;
    private ParticipantKind kind;
    private String packageName;
    private String type; // not clear

    public Participant(String id, String name, ParticipantKind kind, String packageName, String type) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.packageName = packageName;
        this.type = type;
    }

    public String getPackageName() {
        return packageName;
    }

    public Participant(String id, String name, ParticipantKind kind, String packageName) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.packageName = packageName;
    }

    public String getId() {
        return id;
    }

    public ParticipantKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }
}
