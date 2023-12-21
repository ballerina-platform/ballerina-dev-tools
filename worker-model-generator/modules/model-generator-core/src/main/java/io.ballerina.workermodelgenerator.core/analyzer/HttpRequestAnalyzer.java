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
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.workermodelgenerator.core.CommonUtils;
import io.ballerina.workermodelgenerator.core.NodeBuilder;
import io.ballerina.workermodelgenerator.core.model.Endpoint;
import io.ballerina.workermodelgenerator.core.model.properties.NodeProperties;

import java.util.Map;
import java.util.Optional;

/**
 * Syntax tree analyzer to obtain information from a http request node.
 *
 * @since 2201.9.0
 */
public class HttpRequestAnalyzer extends Analyzer {

    private String path;
    private String endpointName;
    private String action;
    private String outputType;

    protected HttpRequestAnalyzer(NodeBuilder nodeBuilder,
                                  SemanticModel semanticModel,
                                  ModulePartNode modulePartNode, Map<String, String> endpointMap) {
        super(nodeBuilder, semanticModel, modulePartNode, endpointMap);
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        Optional<TypeSymbol> typeSymbol = this.semanticModel.typeOf(checkExpressionNode);
        this.outputType = typeSymbol.isPresent() ? getTypeName(typeSymbol.get()) : TypeDescKind.NONE.getName();
        checkExpressionNode.expression().accept(this);
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        remoteMethodCallActionNode.expression().accept(this);
        if (remoteMethodCallActionNode.arguments().isEmpty()) {
            return;
        }
        remoteMethodCallActionNode.arguments().get(0).accept(this);
        this.action = remoteMethodCallActionNode.methodName().name().text();
    }

    @Override
    public void visit(SimpleNameReferenceNode simpleNameReferenceNode) {
        this.endpointName = simpleNameReferenceNode.name().text();
    }

    @Override
    public void visit(PositionalArgumentNode positionalArgumentNode) {
        positionalArgumentNode.expression().accept(this);
    }

    @Override
    public void visit(BasicLiteralNode basicLiteralNode) {
        this.path = CommonUtils.removeQuotes(basicLiteralNode.literalToken().text());
    }

    @Override
    public NodeProperties buildProperties() {
        String baseUrl = this.endpointMap.get(this.endpointName);
        Endpoint endpoint = new Endpoint(this.endpointName, baseUrl);

        NodeProperties.NodePropertiesBuilder propertiesBuilder = new NodeProperties.NodePropertiesBuilder();
        return propertiesBuilder
                .setAction(this.action)
                .setPath(this.path)
                .setOutputType(this.outputType)
                .setEndpoint(endpoint)
                .build();
    }
}
