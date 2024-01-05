package io.ballerina.workermodelgenerator.core.model.properties;

import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.Endpoint;

import java.util.List;
import java.util.Objects;

public class NodeProperties {

    // Switch node properties
    List<SwitchCase> cases;
    SwitchDefaultCase defaultCase;

    // Code node properties
    BalExpression codeBlock;

    // Transform node properties
    String outputType;
    BalExpression expression;
    CodeLocation transformFunctionLocation;

    // HTTP request node properties
    String action;
    String path;
    Endpoint endpoint;

    private NodeProperties() {
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof NodeProperties that)) {
            return false;
        }
        return Objects.equals(this.cases, that.cases) &&
                Objects.equals(this.defaultCase, that.defaultCase) &&
                Objects.equals(this.codeBlock, that.codeBlock) &&
                Objects.equals(this.outputType, that.outputType) &&
                Objects.equals(this.expression, that.expression) &&
                Objects.equals(this.transformFunctionLocation, that.transformFunctionLocation) &&
                Objects.equals(this.action, that.action) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cases, defaultCase, codeBlock, outputType, expression, transformFunctionLocation, action,
                path, endpoint);
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

        public NodePropertiesBuilder setExpression(BalExpression expression) {
            nodeProperties.expression = expression;
            return this;
        }

        public NodePropertiesBuilder setOutputType(String outputType) {
            nodeProperties.outputType = outputType;
            return this;
        }

        public NodePropertiesBuilder setTransformFunctionLocation(CodeLocation transformFunctionLocation) {
            nodeProperties.transformFunctionLocation = transformFunctionLocation;
            return this;
        }

        public NodePropertiesBuilder setAction(String action) {
            nodeProperties.action = action;
            return this;
        }

        public NodePropertiesBuilder setPath(String path) {
            nodeProperties.path = path;
            return this;
        }

        public NodePropertiesBuilder setEndpoint(Endpoint endpoint) {
            nodeProperties.endpoint = endpoint;
            return this;
        }

        public NodeProperties build() {
            return nodeProperties;
        }
    }
}
