package io.ballerina.workermodelgenerator.core.model;

import java.util.List;

public record SwitchProperties(List<SwitchCase> cases, SwitchDefaultCase defaultCase) {
}
