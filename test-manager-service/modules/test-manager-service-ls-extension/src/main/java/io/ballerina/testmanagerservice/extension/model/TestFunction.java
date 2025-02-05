/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.testmanagerservice.extension.model;

import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.tools.text.LineRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Represents a test function.
 *
 * @since 2.0.0
 */
public record TestFunction(Metadata metadata, Codedata codedata, Property functionName, Property returnType,
                           List<FunctionParameter> parameters, List<Annotation> annotations,
                           boolean editable) {

    public static List<FunctionParameter> parameters(SeparatedNodeList<ParameterNode> parameters) {
        List<FunctionParameter> functionParameters = new ArrayList<>();
        for (ParameterNode parameter : parameters) {
            if (parameter instanceof RequiredParameterNode requiredParameterNode) {
                if (requiredParameterNode.paramName().isEmpty()) {
                    continue;
                }
                String paramName = requiredParameterNode.paramName().get().text().trim();
                String paramType = requiredParameterNode.typeName().toSourceCode().trim();
                FunctionParameter.FunctionParameterBuilder builder = new FunctionParameter.FunctionParameterBuilder();
                builder.type(paramType);
                builder.variable(paramName);
                functionParameters.add(builder.build());
            } else if (parameter instanceof DefaultableParameterNode defaultableParameterNode) {
                if (defaultableParameterNode.paramName().isEmpty()) {
                    continue;
                }
                String paramName = defaultableParameterNode.paramName().get().text().trim();
                String paramType = defaultableParameterNode.typeName().toSourceCode().trim();
                String defaultValue = defaultableParameterNode.expression().toSourceCode().trim();
                FunctionParameter.FunctionParameterBuilder builder = new FunctionParameter.FunctionParameterBuilder();
                builder.type(paramType);
                builder.variable(paramName);
                builder.defaultValue(defaultValue);
                functionParameters.add(builder.build());
            }
        }
        return functionParameters;
    }

    public static Property returnType(Optional<ReturnTypeDescriptorNode> returnTypeDescriptorNode) {
        Property.PropertyBuilder builder = new Property.PropertyBuilder();
        builder.metadata(new Metadata("Return Type", "Return type of the function"));
        builder.valueType("TYPE");
        builder.optional(true);
        builder.advanced(true);
        builder.editable(true);
        returnTypeDescriptorNode.ifPresent(
                typeDescriptorNode -> builder.value(typeDescriptorNode.type().toSourceCode().trim()));
        return builder.build();
    }

    public static Property functionName(String functionName) {
        Property.PropertyBuilder builder = new Property.PropertyBuilder();
        builder.metadata(new Metadata("Test Function", "Test function"));
        builder.valueType("IDENTIFIER");
        builder.value(functionName);
        builder.advanced(false);
        builder.editable(true);
        return builder.build();
    }

    public static class FunctionBuilder {
        private Metadata metadata;
        private Codedata codedata;
        private Property functionName;
        private Property returnType;
        private List<FunctionParameter> parameters;
        private List<Annotation> annotations;
        private boolean editable;

        public FunctionBuilder metadata(Metadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public FunctionBuilder codedata(Codedata codedata) {
            this.codedata = codedata;
            return this;
        }

        public FunctionBuilder functionName(Property functionName) {
            this.functionName = functionName;
            return this;
        }

        public FunctionBuilder returnType(Property returnType) {
            this.returnType = returnType;
            return this;
        }

        public FunctionBuilder parameters(List<FunctionParameter> parameters) {
            this.parameters = parameters;
            return this;
        }

        public FunctionBuilder annotations(List<Annotation> annotations) {
            this.annotations = annotations;
            return this;
        }

        public FunctionBuilder editable(boolean editable) {
            this.editable = editable;
            return this;
        }

        public TestFunction build() {
            return new TestFunction(metadata, codedata, functionName, returnType, parameters, annotations, editable);
        }
    }
}
