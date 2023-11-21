package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import static io.ballerina.sequencemodelgenerator.core.model.Constants.PARTICIPANT;

public class Participant extends DElement {
    private final String id;
    private final String name;
    private final ParticipantKind participantKind;
    private final String packageName;
    private String type;

    public Participant(String id, String name, ParticipantKind kind, String packageName, String type, LineRange location) {
        super(PARTICIPANT, false, location);
        this.id = id;
        this.name = name;
        this.participantKind = kind;
        this.packageName = packageName;
        this.type = type;
    }

    public String getPackageName() {
        return packageName;
    }

    public Participant(String id, String name, ParticipantKind kind, String packageName, LineRange location) {
        super("Participant", false, location);
        this.id = id;
        this.name = name;
        this.participantKind = kind;
        this.packageName = packageName;
    }

    public String getId() {
        return id;
    }

    public ParticipantKind getParticipantKind() {
        return participantKind;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", kind=" + participantKind +
                ", packageName='" + packageName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
