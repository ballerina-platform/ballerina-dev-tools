package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.sequencemodelgenerator.core.model.DElement;
import io.ballerina.sequencemodelgenerator.core.model.Participant;

import java.util.ArrayList;
import java.util.List;

public class VisitorContext {
    private Participant rootParticipant;
    private Participant currentParticipant;
    private final List<Participant> participants;
    private DElement diagramElementWithChildren;
    private final List<String> visitedFunctionNames;

    public VisitorContext() {
        this.rootParticipant = null;
        this.currentParticipant = null;
        this.participants = new ArrayList<>();
        this.visitedFunctionNames = new ArrayList<>();
    }

    public VisitorContext(Participant rootParticipant, Participant participant, List<Participant> participants, List<String> visitedFunctionNames) {
        this.rootParticipant = rootParticipant;
        this.currentParticipant = participant;
        this.participants = participants;
        this.visitedFunctionNames = visitedFunctionNames;
    }

    public VisitorContext(Participant rootParticipant, Participant currentParticipant, List<Participant> participants, DElement diagramElementWithChildren, List<String> visitedFunctionNames) {
        this.rootParticipant = rootParticipant;
        this.currentParticipant = currentParticipant;
        this.participants = participants;
        this.diagramElementWithChildren = diagramElementWithChildren;
        this.visitedFunctionNames = visitedFunctionNames;
    }

    public DElement getDiagramElementWithChildren() {
        return diagramElementWithChildren;
    }

    public Participant getCurrentParticipant() {
        return currentParticipant;
    }

    public Participant getRootParticipant() {
        return rootParticipant;
    }

    public void setCurrentParticipant(Participant currentParticipant) {
        this.currentParticipant = currentParticipant;
    }

    public void setRootParticipant(Participant rootParticipant) {
        this.rootParticipant = rootParticipant;
    }

    public List<Participant> getParticipants() {
        return participants;
    }

    public void addToParticipants(Participant participant) {
        this.participants.add(participant);
    }

    public List<String> getVisitedFunctionNames() {
        return visitedFunctionNames;
    }

    public void addToVisitedFunctionNames(String nameReferenceNode) {
        this.visitedFunctionNames.add(nameReferenceNode);
    }
}
