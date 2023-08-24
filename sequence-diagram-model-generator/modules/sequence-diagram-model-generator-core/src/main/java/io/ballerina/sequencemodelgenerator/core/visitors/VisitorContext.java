package io.ballerina.sequencemodelgenerator.core.visitors;

import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.sequencemodelgenerator.core.model.DiagramElementWithChildren;
import io.ballerina.sequencemodelgenerator.core.model.Participant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisitorContext {
    private Participant rootParticipant;
    private Participant currentParticipant;
    private List<Participant> participants;

    private DiagramElementWithChildren diagramElementWithChildren;

    public DiagramElementWithChildren getDiagramElementWithChildren() {
        return diagramElementWithChildren;
    }

    private Set<NameReferenceNode> visitedFunctionNames;

    public VisitorContext() {
        this.rootParticipant = null;
        this.currentParticipant = null;
        this.participants = new ArrayList<>();
        this.visitedFunctionNames = new HashSet<>();
    }

    public VisitorContext(Participant rootParticipant, Participant participant, List<Participant> participants, Set<NameReferenceNode> visitedFunctionNames) {
        this.rootParticipant = rootParticipant;
        this.currentParticipant = participant;
        this.participants = participants;
        this.visitedFunctionNames = visitedFunctionNames;
    }

    public VisitorContext(Participant rootParticipant, Participant currentParticipant, List<Participant> participants, DiagramElementWithChildren diagramElementWithChildren, Set<NameReferenceNode> visitedFunctionNames) {
        this.rootParticipant = rootParticipant;
        this.currentParticipant = currentParticipant;
        this.participants = participants;
        this.diagramElementWithChildren = diagramElementWithChildren;
        this.visitedFunctionNames = visitedFunctionNames;
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

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public void addToParticipants(Participant participant) {
        this.participants.add(participant);
    }

    public Set<NameReferenceNode> getVisitedFunctionNames() {
        return visitedFunctionNames;
    }

    public void setVisitedFunctionNames(Set<NameReferenceNode> visitedFunctionNames) {
        this.visitedFunctionNames = visitedFunctionNames;
    }

    public void addToVisitedFunctionNames(NameReferenceNode nameReferenceNode) {
        this.visitedFunctionNames.add(nameReferenceNode);
    }
}
