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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.model.node.CallNode;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.IfNode;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingOptions;
import org.ballerinalang.formatter.core.FormattingTreeModifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a node in the flow model.
 *
 * @since 2201.9.0
 */
public abstract class FlowNode {

    private String id;
    private String label;
    private LineRange lineRange;
    private Kind kind;
    private boolean returning;
    private boolean fixed;
    private List<Branch> branches;
    private Map<String, Expression> nodeProperties;
    private int flags;

    protected FlowNode(String label, Kind kind, boolean fixed, Map<String, Expression> nodeProperties) {
        this.label = label;
        this.kind = kind;
        this.fixed = fixed;
        if (!nodeProperties.isEmpty()) {
            this.nodeProperties = nodeProperties;
        }
    }

    private void setCommonFields(LineRange lineRange, boolean returning, List<Branch> branches, int flags) {
        this.id = String.valueOf(Objects.hash(lineRange));
        this.lineRange = lineRange;
        this.returning = returning;
        this.branches = branches;
        this.flags = flags;
    }

    public Kind kind() {
        return kind;
    }

    protected Expression getProperty(String key) {
        return nodeProperties != null ? nodeProperties.get(key) : null;
    }

    protected Branch getBranch(String label) {
        return branches.stream().filter(branch -> branch.label().equals(label)).findFirst().orElse(null);
    }

    public LineRange lineRange() {
        return lineRange;
    }

    public boolean hasFlag(int flag) {
        return (flags & flag) == flag;
    }

    public boolean returning() {
        return returning;
    }

    public abstract String toSource();

    public static int NODE_FLAG_CHECKED = 1 << 0;
    public static int NODE_FLAG_CHECKPANIC = 1 << 1;
    public static int NODE_FLAG_FINAL = 1 << 2;
    public static int NODE_FLAG_REMOTE = 1 << 10;
    public static int NODE_FLAG_RESOURCE = 1 << 11;

    public enum Kind {
        EVENT_HTTP_API,
        IF,
        HTTP_API_GET_CALL,
        HTTP_API_POST_CALL,
        RETURN,
        EXPRESSION
    }

    /**
     * Represents a builder for the flow node.
     *
     * @since 2201.9.0
     */
    public final static class NodeBuilder {

        private LineRange lineRange;
        private boolean returning;
        private final List<Branch> branches;
        private int flags;
        private NodePropertiesBuilder nodePropertiesBuilder;
        private final SemanticModel semanticModel;

        public NodeBuilder(SemanticModel semanticModel) {
            this.branches = new ArrayList<>();
            this.flags = 0;
            this.semanticModel = semanticModel;
        }

        public NodeBuilder returning() {
            this.returning = true;
            return this;
        }

        public NodeBuilder lineRange(Node node) {
            if (this.lineRange == null) {
                this.lineRange = node.lineRange();
            }
            return this;
        }

        public NodeBuilder branch(String label, Branch.BranchKind kind, List<FlowNode> children) {
            this.branches.add(new Branch(label, kind, children));
            return this;
        }

        public NodeBuilder flag(int flag) {
            this.flags |= flag;
            return this;
        }

        public NodeBuilder propertiesBuilder(NodePropertiesBuilder propertiesBuilder) {
            this.nodePropertiesBuilder = propertiesBuilder;
            return this;
        }

        public boolean isDefault() {
            return this.nodePropertiesBuilder == null;
        }

        public FlowNode build() {
            if (nodePropertiesBuilder == null) {
                this.nodePropertiesBuilder = new DefaultExpression.Builder(semanticModel);
            }
            FlowNode node = nodePropertiesBuilder.build();
            List<Branch> outBranches = branches.isEmpty() ? null : branches;
            node.setCommonFields(lineRange, returning, outBranches, flags);
            return node;
        }
    }

    public abstract static class NodePropertiesBuilder {

        private static final String VARIABLE_LABEL = "Variable";
        public static final String VARIABLE_KEY = "variable";
        private static final String VARIABLE_DOC = "Result Variable";

        public final static String EXPRESSION_RHS_LABEL = "Expression";
        public final static String EXPRESSION_RHS_KEY = "expression";
        public final static String EXPRESSION_RHS_DOC = "Expression";

        protected final Map<String, Expression> nodeProperties;
        protected final SemanticModel semanticModel;
        protected Expression.Builder expressionBuilder;

        protected Expression variable;
        protected Expression expression;

        public NodePropertiesBuilder(SemanticModel semanticModel) {
            this.nodeProperties = new LinkedHashMap<>();
            this.expressionBuilder = new Expression.Builder();
            this.semanticModel = semanticModel;
        }

        @SuppressWarnings("unchecked")
        public <T extends NodePropertiesBuilder> T variable(Node node) {
            if (node == null) {
                return (T) this;
            }
            if (node.kind() == SyntaxKind.TYPED_BINDING_PATTERN) {
                TypedBindingPatternNode typedBindingPatternNode = (TypedBindingPatternNode) node;
                BindingPatternNode bindingPatternNode = typedBindingPatternNode.bindingPattern();

                expressionBuilder
                        .label(VARIABLE_LABEL)
                        .value(bindingPatternNode.toString())
                        .editable()
                        .typeKind(Expression.ExpressionTypeKind.BTYPE)
                        .documentation(VARIABLE_DOC);

                Optional<Symbol> typeDescriptorSymbol = semanticModel.symbol(typedBindingPatternNode.typeDescriptor());
                if (typeDescriptorSymbol.isPresent() && typeDescriptorSymbol.get().kind() == SymbolKind.TYPE) {
                    TypeSymbol typeSymbol = (TypeSymbol) typeDescriptorSymbol.get();
                    expressionBuilder.type(typeSymbol);
                } else {
                    Optional<Symbol> bindingPatternSymbol = semanticModel.symbol(bindingPatternNode);
                    if (bindingPatternSymbol.isPresent() && bindingPatternSymbol.get().kind() == SymbolKind.VARIABLE) {
                        expressionBuilder.type(((VariableSymbol) bindingPatternSymbol.get()).typeDescriptor());
                    }
                }
            } else {
                semanticModel.typeOf(node).ifPresent(expressionBuilder::type);
                expressionBuilder
                        .label(VARIABLE_LABEL)
                        .value(node.toString().strip())
                        .editable()
                        .typeKind(Expression.ExpressionTypeKind.BTYPE)
                        .documentation(VARIABLE_DOC);
            }

            this.variable = expressionBuilder.build();
            addProperty(VARIABLE_KEY, this.variable);
            return (T) this;
        }

        public NodePropertiesBuilder expression(ExpressionNode expression) {
            semanticModel.typeOf(expression).ifPresent(expressionBuilder::type);
            this.expression = expressionBuilder
                    .label(EXPRESSION_RHS_LABEL)
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .documentation(EXPRESSION_RHS_DOC)
                    .editable()
                    .value(expression.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expression).expression().toString() : expression.toString())
                    .build();
            return this;
        }

        public final void addProperty(String key, Expression expression) {
            if (expression != null) {
                this.nodeProperties.put(key, expression);
            }
        }

        public abstract FlowNode build();
    }

    public static class SourceBuilder {

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

        public SourceBuilder expression(Expression expression) {
            sb.append(expression.toSourceCode());
            return this;
        }

        public SourceBuilder expressionWithType(Expression expression) {
            sb.append(expression.type()).append(WHITE_SPACE).append(expression.toSourceCode());
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
            flowNodes.forEach(flowNode -> sb.append(flowNode.toSource()));
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
    }

    /**
     * Represents a deserializer for the flow node.
     *
     * @since 2201.9.0
     */
    public static class Deserializer implements JsonDeserializer<FlowNode> {

        @Override
        public FlowNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            FlowNode.Kind kind = context.deserialize(jsonObject.get("kind"), FlowNode.Kind.class);

            return switch (kind) {
                case EXPRESSION -> context.deserialize(jsonObject, DefaultExpression.class);
                case IF -> context.deserialize(jsonObject, IfNode.class);
                case EVENT_HTTP_API -> context.deserialize(jsonObject, HttpApiEvent.class);
                case RETURN -> context.deserialize(jsonObject, Return.class);
                case HTTP_API_GET_CALL, HTTP_API_POST_CALL -> context.deserialize(jsonObject, CallNode.class);
            };
        }
    }
}