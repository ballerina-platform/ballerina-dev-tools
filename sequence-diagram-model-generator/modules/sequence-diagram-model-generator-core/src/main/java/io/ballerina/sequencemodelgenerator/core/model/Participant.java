package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;
import java.util.Objects;

public class Participant {
    private String id;
    private String name;
    private ParticipantKind kind;
    private String packageName;
    private String type; // not clear

    private List<Statement> statements;

    public List<Statement> getStatements() {
        return statements;
    }

    public void addStatement(Statement statement) {
        if (this.statements == null) {
            this.statements = new java.util.ArrayList<>();
            this.statements.add(statement);
        } else {
            this.statements.add(statement);
        }
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements;
    }

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

    @Override
    public String toString() {
        return "Participant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", kind=" + kind +
                ", packageName='" + packageName + '\'' +
                ", type='" + type + '\'' +
                ", statements=" + statements +
                '}';
    }
}
