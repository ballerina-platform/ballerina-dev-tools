package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

/**
 * Represents the sequence model.
 *
 * @since 2201.8.0
 */
public class SequenceModel extends DNode {
    private final List<Participant> participants;

    public SequenceModel(List<Participant> participants, LineRange location) {
        super("Sequence", false, location);
        this.participants = participants;
    }

    public List<Participant> getParticipants() {
        return participants;
    }
}
