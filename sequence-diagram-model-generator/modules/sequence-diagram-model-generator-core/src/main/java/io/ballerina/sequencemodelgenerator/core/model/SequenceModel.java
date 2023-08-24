package io.ballerina.sequencemodelgenerator.core.model;

import java.util.List;

public class SequenceModel extends DiagramElement {
    List<Participant> participants;

    public SequenceModel(List<Participant> participants) {
        super("Sequence");
        this.participants = participants;
    }
}
