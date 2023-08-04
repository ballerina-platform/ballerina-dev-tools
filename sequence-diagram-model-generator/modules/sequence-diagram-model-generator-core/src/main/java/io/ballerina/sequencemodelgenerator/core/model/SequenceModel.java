package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class SequenceModel {
    private List<Participant> participants;
    private List<Interaction> interactions;

    public SequenceModel(List<Participant> participants, List<Interaction> interactions) {
        this.participants = participants;
        this.interactions = interactions;
    }
}
