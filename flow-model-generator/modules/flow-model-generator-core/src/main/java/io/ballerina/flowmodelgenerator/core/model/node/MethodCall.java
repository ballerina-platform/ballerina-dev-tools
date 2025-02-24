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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionResult;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a method call.
 *
 * @since 2.0.0
 */
public class MethodCall extends FunctionBuilder {

    @Override
    protected Map<Path, List<TextEdit>> buildFunctionCall(SourceBuilder sourceBuilder, FlowNode flowNode) {
        Optional<Property> connection = flowNode.getProperty(Property.CONNECTION_KEY);
        if (connection.isEmpty()) {
            throw new IllegalStateException("Object must be defined for a method call node");
        }

        return sourceBuilder.token()
                .name(connection.get().toSourceCode())
                .keyword(SyntaxKind.DOT_TOKEN)
                .name(flowNode.metadata().label())
                .stepOut()
                .functionParameters(flowNode, Set.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY, Property.TYPE_KEY,
                        Property.CHECK_ERROR_KEY, "view"))
                .textEdit(false)
                .acceptImport(sourceBuilder.filePath)
                .build();
    }

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.METHOD_CALL;
    }

    @Override
    protected FunctionResult.Kind getFunctionResultKind() {
        return FunctionResult.Kind.FUNCTION;
    }
}
