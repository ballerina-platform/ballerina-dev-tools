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

package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Visitor for finding annotations of a given node.
 *
 * @since 2201.9.0
 */
class AnnotationFinder extends NodeVisitor {

    private String valueHolder;
    private final Map<String, String> annotationConfig;

    public AnnotationFinder() {
        this.annotationConfig = new HashMap<>();
    }

    public Map<String, String> getAnnotationConfig() {
        return annotationConfig;
    }

    @Override
    public void visit(AnnotationNode annotationNode) {
        Optional<MappingConstructorExpressionNode> mappingConstructorExpressionNode = annotationNode.annotValue();
        mappingConstructorExpressionNode.ifPresent(annotationValueNode -> annotationValueNode.accept(this));
    }

    @Override
    public void visit(MappingConstructorExpressionNode mappingConstructorExpressionNode) {
        mappingConstructorExpressionNode.fields().forEach(mappingFieldNode -> mappingFieldNode.accept(this));
    }

    @Override
    public void visit(SpecificFieldNode specificFieldNode) {
        String fieldName = specificFieldNode.fieldName().kind() == SyntaxKind.IDENTIFIER_TOKEN ?
                ((IdentifierToken) specificFieldNode.fieldName()).text() : "";
        specificFieldNode.valueExpr().ifPresent(expressionNode -> expressionNode.accept(this));
        String fieldValue = this.valueHolder;

        annotationConfig.put(fieldName, fieldValue);
    }

    @Override
    public void visit(BasicLiteralNode basicLiteralNode) {
        Token valueToken = basicLiteralNode.literalToken();
        this.valueHolder = valueToken.kind() == SyntaxKind.STRING_LITERAL_TOKEN ?
                CommonUtils.removeQuotes(valueToken.text()) : valueToken.text();
    }
}
