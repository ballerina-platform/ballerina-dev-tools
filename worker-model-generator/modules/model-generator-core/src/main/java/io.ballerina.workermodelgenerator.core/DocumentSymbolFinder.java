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


package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The syntax tree analyzer to find the information about the symbols in the document.
 *
 * @since 2201.9.0
 */
class DocumentSymbolFinder extends NodeVisitor {

    private final Map<String, String> endpointMap;
    private String endpoint;
    private String baseUrl;

    public DocumentSymbolFinder() {
        this.endpointMap = new HashMap<>();
    }

    public Map<String, String> getEndpointMap() {
        return endpointMap;
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        moduleVariableDeclarationNode.typedBindingPattern().accept(this);
        if (moduleVariableDeclarationNode.initializer().isEmpty()) {
            return;
        }
        moduleVariableDeclarationNode.initializer().get().accept(this);
        this.endpointMap.put(this.endpoint, this.baseUrl);
        this.endpoint = "";
        this.baseUrl = "";
    }

    @Override
    public void visit(TypedBindingPatternNode typedBindingPatternNode) {
        typedBindingPatternNode.bindingPattern().accept(this);
    }

    @Override
    public void visit(CaptureBindingPatternNode captureBindingPatternNode) {
        this.endpoint = captureBindingPatternNode.variableName().text();
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        super.visit(checkExpressionNode);
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        Optional<ParenthesizedArgList> parenthesizedArgList = implicitNewExpressionNode.parenthesizedArgList();
        if (parenthesizedArgList.isEmpty() || parenthesizedArgList.get().arguments().isEmpty()) {
            return;
        }
        parenthesizedArgList.get().arguments().get(0).accept(this);
    }

    @Override
    public void visit(PositionalArgumentNode positionalArgumentNode) {
        positionalArgumentNode.expression().accept(this);
    }

    @Override
    public void visit(BasicLiteralNode basicLiteralNode) {
        this.baseUrl = CommonUtils.removeQuotes(basicLiteralNode.literalToken().text());
    }
}
