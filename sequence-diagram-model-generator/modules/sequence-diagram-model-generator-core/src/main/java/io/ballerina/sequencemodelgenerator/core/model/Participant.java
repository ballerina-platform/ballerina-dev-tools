package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import static io.ballerina.sequencemodelgenerator.core.model.Constants.PARTICIPANT;

public class Participant extends DElement {
    private final String id;
    private final String name;
    private final ParticipantKind participantKind;
    private final String packageName;
    private String type;
    private boolean hasInteractions;

    public Participant(String id, String name, ParticipantKind kind, String packageName, String type, LineRange location, boolean hasInteractions) {
        super(PARTICIPANT, false, location);
        this.id = id;
        this.name = name;
        this.participantKind = kind;
        this.packageName = packageName;
        this.type = type;
        this.hasInteractions = hasInteractions;
    }

    public Participant(String id, String name, ParticipantKind kind, String packageName, LineRange location) {
        super(PARTICIPANT, false, location);
        this.id = id;
        this.name = name;
        this.participantKind = kind;
        this.packageName = packageName;
    }

    public void setHasInteractions(boolean hasInteractions) {
        this.hasInteractions = hasInteractions;
    }

    public String getPackageName() {
        return packageName;
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
                ", hasInteractions=" + hasInteractions +
                '}';
    }
}
