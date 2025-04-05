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

import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Branch;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents the properties of a switch node in the flow model.
 *
 * @since 2.0.0
 */
public class MatchBuilder extends NodeBuilder {

    public static final String LABEL = "Match";
    public static final String DESCRIPTION = "Switches the data flow based on the value of an expression.";

    @Override
    public void setConcreteConstData() {
        metadata().label(LABEL).description(DESCRIPTION);
        codedata().node(NodeKind.MATCH);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        properties().condition(null);

        Branch.Builder defaultCaseBuilder = new Branch.Builder()
                .codedata().node(NodeKind.CONDITIONAL).stepOut()
                .label("case")
                .kind(Branch.BranchKind.BLOCK)
                .repeatable(Branch.Repeatable.ONE_OR_MORE)
                .properties().patterns(NodeFactory.createEmptyNodeList())
                .stepOut();
        this.branches = List.of(defaultCaseBuilder.build(), Branch.getDefaultOnFailBranch(true));
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        Optional<Property> condition = sourceBuilder.getProperty(Property.CONDITION_KEY);
        if (condition.isEmpty()) {
            throw new IllegalStateException("Match node does not have a condition");
        }

        sourceBuilder.token()
                .keyword(SyntaxKind.MATCH_KEYWORD)
                .expression(condition.get())
                .stepOut()
                .token().openBrace();

        for (Branch branch : sourceBuilder.flowNode.branches()) {
            Optional<Property> patterns = branch.getProperty(Property.PATTERNS_KEY);
            if (patterns.isEmpty()) {
                continue;
            }

            List<Property> patternsList = patterns.get().valueAsType(Property.LIST_PROPERTY_TYPE_TOKEN);
            String joinedPatterns = patternsList.stream()
                    .map(Property::toSourceCode)
                    .collect(Collectors.joining("|"));

            sourceBuilder.token().name(joinedPatterns);

            Optional<Property> guardProperty = branch.getProperty(Property.GUARD_KEY);
            guardProperty.ifPresent(property -> sourceBuilder.token()
                    .keyword(SyntaxKind.IF_KEYWORD)
                    .whiteSpace()
                    .expression(property));

            sourceBuilder.token()
                    .keyword(SyntaxKind.RIGHT_DOUBLE_ARROW_TOKEN)
                    .openBrace();

            sourceBuilder.children(branch.children());
            sourceBuilder.token().closeBrace();
        }

        sourceBuilder.token().closeBrace();
        sourceBuilder.onFailure();
        return sourceBuilder.textEdit().build();
    }
}
