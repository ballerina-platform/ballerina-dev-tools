package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

public class Participant extends DiagramElementWithChildren {
    private final String id;
    private final String name;
    private final ParticipantKind participantKind;
    private final String packageName;
    private String type; // not clear


//    private List<Statement> statements;
//
//    public List<Statement> getStatements() {
//        return statements;
//    }
//
//    public void addStatement(Statement statement) {
//        if (this.statements == null) {
//            this.statements = new java.util.ArrayList<>();
//            this.statements.add(statement);
//        } else {
//            this.statements.add(statement);
//        }
//    }

//    public void setStatements(List<Statement> statements) {
//        this.statements = statements;
//    }

    public Participant(String id, String name, ParticipantKind kind, String packageName, String type, LineRange location) {
        super("Participant", false, location);
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
