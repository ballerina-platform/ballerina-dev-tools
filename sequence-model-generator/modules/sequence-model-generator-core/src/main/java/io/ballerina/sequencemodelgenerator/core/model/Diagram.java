package io.ballerina.sequencemodelgenerator.core.model;

import io.ballerina.tools.text.LineRange;

import java.util.List;

public record Diagram(List<Participant> participants, LineRange location) {

}
