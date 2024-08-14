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

import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.Optional;

/**
 * Represents the properties of a variable declaration node.
 *
 * @since 1.4.0
 */
public class NewData extends NodeBuilder {
    public static final String LABEL = "New Variable";
    public static final String DESCRIPTION = "New variable '%s' with type '%s'";
    public static final String NEW_DATA_EXPRESSION_DOC = "Create new variable";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL);
        codedata().node(FlowNode.Kind.NEW_DATA);
    }

    @Override
    public String toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        Optional<Property> exprProperty = sourceBuilder.flowNode.getProperty(Property.EXPRESSION_KEY);
        exprProperty.ifPresent(value -> sourceBuilder.token().expression(value).endOfStatement());

        return sourceBuilder.build(false);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        metadata().description(String.format(DESCRIPTION, "name", "var"));
        properties().defaultDataVariable().defaultExpression(NEW_DATA_EXPRESSION_DOC);
    }
}
