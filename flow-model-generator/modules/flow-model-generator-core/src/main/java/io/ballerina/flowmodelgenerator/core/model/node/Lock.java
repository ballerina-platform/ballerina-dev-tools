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

import java.util.List;

/**
 * Represents the properties of lock node in the flow model.
 *
 * @since 1.4.0
 */
public class Lock extends FlowNode {

    public static final String LABEL = "Lock";
    public static final String DESCRIPTION = "Allow to access mutable states safely";

    @Override
    public void setConstData() {
        this.label = LABEL;
        this.kind = Kind.LOCK;
        this.description = DESCRIPTION;
    }

    @Override
    public String toSource() {
        SourceBuilder sourceBuilder = new SourceBuilder();
        Branch body = getBranch(Branch.BODY_LABEL);
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

    @Override
    public void setConcreteTemplateData() {
        this.branches = List.of(Branch.DEFAULT_BODY_BRANCH, Branch.DEFAULT_ON_FAIL_BRANCH);
    }
}
