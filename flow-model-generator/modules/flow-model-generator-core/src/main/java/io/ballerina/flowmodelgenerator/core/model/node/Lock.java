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
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.Expression;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.tools.text.LineRange;

import java.util.List;
import java.util.Map;

/**
 * Represents the properties of lock node in the flow model.
 *
 * @since 1.4.0
 */
public class Lock extends FlowNode {

    public static final String LOCK_LABEL = "Lock";
    public static final String LOCK_BODY = "Body";
    public static final FlowNode DEFAULT_NODE = new Lock(DEFAULT_ID, LOCK_LABEL, Kind.LOCK, false, Map.of(),
            null, false, List.of(Branch.DEFAULT_BODY_BRANCH, Branch.DEFAULT_ON_FAIL_BRANCH), 0);

    public Lock(String id, String label, Kind kind, boolean fixed, Map<String, Expression> nodeProperties,
                LineRange lineRange, boolean returning, List<Branch> branches, int flags) {
        super(id, label, kind, fixed, nodeProperties, lineRange, returning, branches, flags);
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Branch body = getBranch(LOCK_BODY);
        sourceBuilder
                .keyword(SyntaxKind.LOCK_KEYWORD)
                .openBrace()
                .addChildren(body.children())
                .closeBrace();

        Branch onFailBranch = getBranch(Branch.ON_FAIL_LABEL);
        if (onFailBranch != null) {
            sourceBuilder
                    .keyword(SyntaxKind.ON_KEYWORD)
                    .keyword(SyntaxKind.FAIL_KEYWORD);

            Expression variableProperty = getBranchProperty(onFailBranch, PropertiesBuilder.VARIABLE_KEY);
            if (variableProperty != null) {
                sourceBuilder.expressionWithType(variableProperty);
            }

            sourceBuilder.openBrace()
                    .addChildren(onFailBranch.children())
                    .closeBrace();
        }

        return sourceBuilder.build(false);
    }
}
