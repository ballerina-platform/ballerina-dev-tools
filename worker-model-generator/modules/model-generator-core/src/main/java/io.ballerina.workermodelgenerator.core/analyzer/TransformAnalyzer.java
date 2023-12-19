/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.workermodelgenerator.core.analyzer;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.tools.text.LineRange;
import io.ballerina.workermodelgenerator.core.NodeBuilder;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.properties.BalExpression;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

import java.util.Objects;

/**
 * Syntax tree analyzer to obtain information from a transform node.
 *
 * @since 2201.9.0
 */
public class TransformAnalyzer extends Analyzer {

    private String transformerFunctionName;
    private BalExpression balExpression;

    protected TransformAnalyzer(NodeBuilder nodeBuilder,
                                SemanticModel semanticModel, ModulePartNode modulePartNode) {
        super(nodeBuilder, semanticModel, modulePartNode);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        this.transformerFunctionName =
                ((SimpleNameReferenceNode) functionCallExpressionNode.functionName()).name().text();
        modulePartNode.members().forEach(member -> member.accept(this));
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        if (!Objects.equals(this.transformerFunctionName, functionDefinitionNode.functionName().text())) {
            return;
        }
        LineRange lineRange = functionDefinitionNode.location().lineRange();
        CodeLocation codeLocation = new CodeLocation(lineRange.startLine(), lineRange.endLine());
        this.balExpression = new BalExpression(functionDefinitionNode.toSourceCode(), codeLocation);
    }

    @Override
    public NodeProperties buildProperties() {
        NodeProperties.NodePropertiesBuilder nodePropertiesBuilder = new NodeProperties.NodePropertiesBuilder();
        nodePropertiesBuilder.setCodeBlock(this.balExpression);
        return nodePropertiesBuilder.build();
    }
}
