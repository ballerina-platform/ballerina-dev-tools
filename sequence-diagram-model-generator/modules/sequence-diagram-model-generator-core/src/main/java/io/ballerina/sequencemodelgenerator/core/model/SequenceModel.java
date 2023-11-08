package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

public class SequenceModel extends DNode {
    List<Participant> participants;

    public SequenceModel(List<Participant> participants, LineRange location) {
        super("Sequence", false, location);
        this.participants = participants;
    }
}
