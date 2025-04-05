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
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the properties of an if node in the flow model.
 *
 * @since 2.0.0
 */
public class IfBuilder extends NodeBuilder {

    public static final String LABEL = "If";
    public static final String DESCRIPTION = "Add conditional branch to the integration flow.";
    public static final String IF_THEN_LABEL = "Then";
    public static final String IF_ELSE_LABEL = "Else";
    private static final String IF_CONDITION_DOC = "Boolean Condition";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.IF);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        List<Branch> branches = sourceBuilder.flowNode.branches();
        Optional<Branch> ifBranch = Optional.empty();
        Optional<Branch> elseBranch = Optional.empty();
        List<Branch> remainingBranches = new ArrayList<>();

        for (Branch branch : branches) {
            switch (branch.label()) {
                case IF_THEN_LABEL -> ifBranch = Optional.of(branch);
                case IF_ELSE_LABEL -> elseBranch = Optional.of(branch);
                default -> remainingBranches.add(branch);
            }
        }

        if (ifBranch.isEmpty() || ifBranch.get().getProperty(Property.CONDITION_KEY).isEmpty()) {
            throw new IllegalStateException("If node does not have a valid then branch or condition");
        }

        sourceBuilder
                .token()
                    .keyword(SyntaxKind.IF_KEYWORD)
                    .expression(ifBranch.get().getProperty(Property.CONDITION_KEY).get())
                    .stepOut()
                .body(ifBranch.get().children());

        for (Branch branch : remainingBranches) {
            Optional<Property> branchCondition = branch.getProperty(Property.CONDITION_KEY);
            if (branchCondition.isEmpty()) {
                throw new IllegalStateException("Else-if branch does not have a condition");
            }
            sourceBuilder
                    .token()
                        .keyword(SyntaxKind.ELSE_KEYWORD)
                        .keyword(SyntaxKind.IF_KEYWORD)
                        .expression(branchCondition.get())
                        .stepOut()
                    .body(branch.children());
        }

        elseBranch.ifPresent(branch -> sourceBuilder
                .token()
                    .whiteSpace()
                    .keyword(SyntaxKind.ELSE_KEYWORD)
                    .stepOut()
                .body(branch.children()));

        return sourceBuilder.textEdit().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Branch.Builder thenBranchBuilder = new Branch.Builder()
                .label(IF_THEN_LABEL)
                .kind(Branch.BranchKind.BLOCK)
                .repeatable(Branch.Repeatable.ONE_OR_MORE)
                .codedata().node(NodeKind.CONDITIONAL).stepOut();
        thenBranchBuilder.properties().condition(null);

        this.branches = List.of(thenBranchBuilder.build());
    }
}
