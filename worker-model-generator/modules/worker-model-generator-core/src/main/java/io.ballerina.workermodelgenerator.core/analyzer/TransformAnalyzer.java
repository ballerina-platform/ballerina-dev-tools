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
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.text.LineRange;
import io.ballerina.workermodelgenerator.core.NodeBuilder;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.properties.BalExpression;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Syntax tree analyzer to obtain information from a transform node.
 *
 * @since 2201.9.0
 */
public class TransformAnalyzer extends Analyzer {

    private String transformerFunctionName;
    private BalExpression balExpression;
    private CodeLocation transformFunctionLocation;
    private String outputType;

    protected TransformAnalyzer(NodeBuilder nodeBuilder, SemanticModel semanticModel, ModulePartNode modulePartNode,
                                Map<String, String> endpointMap) {
        super(nodeBuilder, semanticModel, modulePartNode, endpointMap);
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        this.transformerFunctionName =
                ((SimpleNameReferenceNode) functionCallExpressionNode.functionName()).name().text();
        modulePartNode.members().forEach(member -> member.accept(this));

        // Obtain the output type
        Optional<TypeSymbol> typeSymbol = this.semanticModel.typeOf(functionCallExpressionNode);
        this.outputType = typeSymbol.isPresent() ? getTypeName(typeSymbol.get()) : TypeDescKind.NONE.getName();

        // Obtain the bal expression
        NonTerminalNode parent = functionCallExpressionNode.parent();
        while (parent != null && parent.kind() != SyntaxKind.LOCAL_VAR_DECL) {
            parent = parent.parent();
        }
        //TODO: Handle the case when the parent is null

        LineRange lineRange = Objects.requireNonNull(parent).lineRange();
        CodeLocation parentLocation = new CodeLocation(lineRange.startLine(), lineRange.endLine());
        this.balExpression = new BalExpression(parent.toSourceCode(), parentLocation);
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        if (!Objects.equals(this.transformerFunctionName, functionDefinitionNode.functionName().text())) {
            return;
        }
        LineRange lineRange = functionDefinitionNode.location().lineRange();
        this.transformFunctionLocation = new CodeLocation(lineRange.startLine(), lineRange.endLine());
    }

    @Override
    public NodeProperties buildProperties() {
        NodeProperties.NodePropertiesBuilder nodePropertiesBuilder = new NodeProperties.NodePropertiesBuilder();
        nodePropertiesBuilder
                .setOutputType(this.outputType)
                .setExpression(this.balExpression)
                .setTransformFunctionLocation(this.transformFunctionLocation);
        return nodePropertiesBuilder.build();
    }
}
