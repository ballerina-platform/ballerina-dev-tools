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

package io.ballerina.workermodelgenerator.core.analyzer;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ReceiveActionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.workermodelgenerator.core.NodeBuilder;
import io.ballerina.workermodelgenerator.core.model.CodeLocation;
import io.ballerina.workermodelgenerator.core.model.properties.BalExpression;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

import java.util.Map;

/**
 * Syntax tree analyzer to obtain information from a code block node.
 *
 * @since 2201.9.0
 */
public class CodeBlockAnalyzer extends Analyzer {

    private boolean hasProcessed;
    private BalExpression balExpression;

    protected CodeBlockAnalyzer(NodeBuilder nodeBuilder,
                                SemanticModel semanticModel, ModulePartNode modulePartNode,
                                Map<String, String> endpointMap) {
        super(nodeBuilder, semanticModel, modulePartNode, endpointMap);
    }

    @Override
    protected void analyzeSendAction(SimpleNameReferenceNode receiverNode, ExpressionNode expressionNode) {
        super.analyzeSendAction(receiverNode, expressionNode);
        this.hasProcessed = true;
    }

    @Override
    public void visit(ReceiveActionNode receiveActionNode) {
        super.visit(receiveActionNode);
        this.hasProcessed = true;
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        StringBuilder codeBlock = new StringBuilder();
        boolean isStart = true;
        LinePosition start = null;
        LinePosition end = null;
        for (StatementNode statement : blockStatementNode.statements()) {
            statement.accept(this);
            if (!this.hasProcessed) {
                if (isStart) {
                    start = statement.lineRange().startLine();
                    isStart = false;
                }
                end = statement.lineRange().endLine();
                codeBlock.append(statement.toSourceCode());
            }
            this.hasProcessed = false;
        }
        this.balExpression = new BalExpression(codeBlock.toString(), new CodeLocation(start, end));
    }

    @Override
    public NodeProperties buildProperties() {
        NodeProperties.NodePropertiesBuilder nodePropertiesBuilder = new NodeProperties.NodePropertiesBuilder();
        return nodePropertiesBuilder.setCodeBlock(this.balExpression).build();
    }
}
