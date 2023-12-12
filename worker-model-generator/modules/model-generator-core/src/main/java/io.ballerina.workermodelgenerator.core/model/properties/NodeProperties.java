package io.ballerina.workermodelgenerator.core.model.properties;

import io.ballerina.workermodelgenerator.core.model.BalExpression;

import java.util.List;
import java.util.Objects;

public class NodeProperties {

    // Switch node properties
    List<SwitchCase> cases;
    SwitchDefaultCase defaultCase;

    // Code node properties
    BalExpression codeBlock;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NodeProperties that)) {
            return false;
        }
        return Objects.equals(this.cases, that.cases) &&
                Objects.equals(this.defaultCase, that.defaultCase) &&
                Objects.equals(this.codeBlock, that.codeBlock);
    }

    @Override
    public String toString() {
        return String.format("NodeProperties[cases=%s, defaultCase=%s, codeBlock=%s]",
                cases, defaultCase, codeBlock);
    }

    public static class NodePropertiesBuilder {

        private final NodeProperties nodeProperties;

        public NodePropertiesBuilder() {
            nodeProperties = new NodeProperties();
        }

        public NodePropertiesBuilder setSwitchCases(List<SwitchCase> cases) {
            nodeProperties.cases = cases;
            return this;
        }

        public NodePropertiesBuilder setDefaultSwitchCase(SwitchDefaultCase defaultCase) {
            nodeProperties.defaultCase = defaultCase;
            return this;
        }

        public NodePropertiesBuilder setCodeBlock(BalExpression codeBlock) {
            nodeProperties.codeBlock = codeBlock;
            return this;
        }

        public NodeProperties build() {
            return nodeProperties;
        }
    }
}