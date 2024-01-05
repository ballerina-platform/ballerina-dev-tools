package io.ballerina.workermodelgenerator.core.model.properties;

import java.util.List;


public record SwitchCase(BalExpression expression, List<String> nodes) {

}
