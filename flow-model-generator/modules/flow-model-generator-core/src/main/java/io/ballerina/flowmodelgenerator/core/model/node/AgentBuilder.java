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
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.modelgenerator.commons.FunctionData;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents agent node in the flow model.
 *
 * @since 2.0.0
 */
public class AgentBuilder extends CallBuilder {

    private static final String AGENT_LABEL = "Agent";
    public static final String PARAMS_TO_HIDE = "paramsToHide";
    public static final String MODEL = "model";
    public static final String TYPE = "type";
    public static final String TOOLS = "tools";
    public static final String LABEL = "Agent";
    public static final String DESCRIPTION = "Create new agent";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.AGENT;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return FunctionData.Kind.CONNECTOR;
    }

    @Override
    public void setConcreteConstData() {
        metadata().label(AGENT_LABEL);
        codedata().node(NodeKind.AGENT).symbol("init");
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder
                .token().keyword(SyntaxKind.FINAL_KEYWORD).stepOut()
                .newVariable();

        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(sourceBuilder.flowNode, Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY,
                        Property.SCOPE_KEY, Property.CHECK_ERROR_KEY), true);

        return sourceBuilder.textEdit().acceptImport().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        super.setConcreteTemplateData(context);

        metadata().addData(PARAMS_TO_HIDE, List.of(MODEL, TOOLS, TYPE));
    }
}
