/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.workermodelgenerator.core.model.properties;

import io.ballerina.workermodelgenerator.core.model.Endpoint;

import java.util.List;
import java.util.Objects;

/**
 * Represents the properties of a node.
 *
 * @since 2201.9.0
 */
public class NodeProperties {

    // Switch node properties
    List<SwitchCase> cases;
    SwitchDefaultCase defaultCase;

    // Code node properties
    CodeBlock codeBlock;

    // Transform node properties
    String outputType;
    BalExpression expression;
    CodeBlock transformFunction;

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
                Objects.equals(this.transformFunction, that.transformFunction) &&
                Objects.equals(this.action, that.action) &&
                Objects.equals(this.path, that.path) &&
                Objects.equals(this.endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cases, defaultCase, codeBlock, outputType, expression, transformFunction, action,
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

        public NodePropertiesBuilder setCodeBlock(CodeBlock codeBlock) {
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

        public NodePropertiesBuilder setTransformFunction(CodeBlock transformFunction) {
            nodeProperties.transformFunction = transformFunction;
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
