/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.Optional;

/**
 * Represents the properties of a return node.
 *
 * @since 1.4.0
 */
public class Return extends NodeBuilder {

    public static final String LABEL = "Return";
    public static final String DESCRIPTION = "Return a value";
    public static final String RETURN_EXPRESSION_DOC = "Return value";

    @Override
    public void setConcreteConstData() {
        this.label = LABEL;
        this.description = DESCRIPTION;
        this.kind = FlowNode.Kind.RETURN;
    }

    @Override
    public String toSource(FlowNode node) {
        SourceBuilder sourceBuilder = new SourceBuilder();

        sourceBuilder.keyword(SyntaxKind.RETURN_KEYWORD);
        Optional<Property> property = node.getProperty(Property.EXPRESSION_KEY);
        property.ifPresent(value -> sourceBuilder
                .whiteSpace()
                .expression(value));
        sourceBuilder.endOfStatement();
        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData() {
        properties().setDefaultExpression(RETURN_EXPRESSION_DOC);
    }
}
