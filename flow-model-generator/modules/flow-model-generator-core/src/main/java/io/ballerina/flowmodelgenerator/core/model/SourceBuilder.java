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

package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingTreeModifier;
import org.ballerinalang.formatter.core.options.FormattingOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a builder to generate a Ballerina source code.
 *
 * @since 1.4.0
 */
public class SourceBuilder {

    private static final String WHITE_SPACE = " ";

    private static final FormattingTreeModifier
            treeModifier = new FormattingTreeModifier(FormattingOptions.builder().build(), (LineRange) null);
    private final StringBuilder sb;

    public SourceBuilder() {
        sb = new StringBuilder();
    }

    public SourceBuilder keyword(SyntaxKind keyword) {
        sb.append(keyword.stringValue()).append(WHITE_SPACE);
        return this;
    }

    public SourceBuilder name(String name) {
        sb.append(name);
        return this;
    }

    public SourceBuilder expression(Property property) {
        sb.append(property.toSourceCode());
        return this;
    }

    public SourceBuilder expressionWithType(Property type, Property variable) {
        sb.append(type.toSourceCode()).append(WHITE_SPACE).append(variable.toSourceCode());
        return this;
    }

    public SourceBuilder expressionWithType(Property property) {
        sb.append(property.valueType()).append(WHITE_SPACE).append(property.toSourceCode());
        return this;
    }

    public SourceBuilder whiteSpace() {
        sb.append(WHITE_SPACE);
        return this;
    }

    public SourceBuilder openBrace() {
        sb.append(SyntaxKind.OPEN_BRACE_TOKEN.stringValue()).append(System.lineSeparator());
        return this;
    }

    public SourceBuilder closeBrace() {
        sb.append(WHITE_SPACE)
                .append(SyntaxKind.CLOSE_BRACE_TOKEN.stringValue())
                .append(System.lineSeparator());
        return this;
    }

    public SourceBuilder addChildren(List<FlowNode> flowNodes) {
        flowNodes.forEach(
                flowNode -> sb.append(NodeBuilder.getNodeFromKind(flowNode.codedata().node()).toSource(flowNode)));
        return this;
    }

    public SourceBuilder endOfStatement() {
        sb.append(SyntaxKind.SEMICOLON_TOKEN.stringValue()).append(System.lineSeparator());
        return this;
    }

    public String build(boolean isExpression) {
        String outputStr = sb.toString();
        Node modifiedNode = isExpression ? NodeParser.parseExpression(outputStr).apply(treeModifier) :
                NodeParser.parseStatement(outputStr).apply(treeModifier);
        return modifiedNode.toSourceCode().strip();
    }

    /**
     * Contains factory methods to apply common templates on the provided builder.
     *
     * @since 1.4.0
     */
    public static class TemplateFactory {

        /**
         * Adds an <code>on fail</code> block to the provided <code>SourceBuilder</code>.
         * <pre>{@code
         *     on fail <errorType> <errorVariable> {
         *          <statement>...
         *     }
         * }</pre>
         *
         * @param sourceBuilder The source builder to which the <code>on fail</code> block should be added
         */
        public static void addOnFailure(SourceBuilder sourceBuilder, FlowNode flowNode) {
            Optional<Branch> optOnFailureBranch = flowNode.getBranch(Branch.ON_FAILURE_LABEL);
            if (optOnFailureBranch.isEmpty()) {
                return;
            }
            Branch onFailureBranch = optOnFailureBranch.get();

            // Build the keywords
            sourceBuilder
                    .keyword(SyntaxKind.ON_KEYWORD)
                    .keyword(SyntaxKind.FAIL_KEYWORD);

            // Build the parameters
            Optional<Property> onErrorType = onFailureBranch.getProperty(Property.ON_ERROR_TYPE_KEY);
            Optional<Property> onErrorValue = onFailureBranch.getProperty(Property.ON_ERROR_VARIABLE_KEY);
            if (onErrorType.isPresent() && onErrorValue.isPresent()) {
                sourceBuilder.expressionWithType(onErrorType.get(), onErrorValue.get());
            }

            // Build the body
            sourceBuilder.openBrace()
                    .addChildren(onFailureBranch.children())
                    .closeBrace();
        }

        /**
         * Adds function arguments to the provided <code>SourceBuilder</code>. This method processes the properties of
         * the <code>flowNode</code> and adds them as arguments to the <code>sourceBuilder</code>. it skips properties
         * that are either empty or have default values.
         *
         * <pre>{@code
         *  (<mandatory-arg>..., <named_arg>=<default-value>...);
         * }</pre>
         *
         * @param sourceBuilder     The <code>SourceBuilder</code> instance to which the function arguments will be
         *                          added.
         * @param flowNode          The <code>FlowNode</code> instance containing the actual properties.
         * @param nodeTemplate      The <code>FlowNode</code> instance containing the template properties.
         * @param ignoredProperties A set of property keys to be ignored during the processing.
         */
        public static void addFunctionArguments(SourceBuilder sourceBuilder, FlowNode flowNode, FlowNode nodeTemplate,
                                                Set<String> ignoredProperties) {
            sourceBuilder.keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            Set<String> keys = new LinkedHashSet<>(nodeTemplate.properties().keySet());
            keys.removeAll(ignoredProperties);

            boolean hasEmptyParam = false;
            boolean firstParamAdded = false;
            for (String key : keys) {
                Optional<Property> property = flowNode.getProperty(key);
                Optional<Property> templateProperty = nodeTemplate.getProperty(key);

                if (property.isEmpty() || templateProperty.isEmpty() || property.get().value() == null ||
                        (property.get().optional() && property.get().value().equals(templateProperty.get().value()))) {
                    hasEmptyParam = true;
                    continue;
                }

                if (firstParamAdded) {
                    sourceBuilder.keyword(SyntaxKind.COMMA_TOKEN);
                } else {
                    firstParamAdded = true;
                }

                if (hasEmptyParam) {
                    sourceBuilder
                            .name(key)
                            .keyword(SyntaxKind.EQUAL_TOKEN);
                    hasEmptyParam = false;
                }

                sourceBuilder.expression(property.get());
            }

            sourceBuilder
                    .keyword(SyntaxKind.CLOSE_PAREN_TOKEN)
                    .endOfStatement();
        }
    }
}
