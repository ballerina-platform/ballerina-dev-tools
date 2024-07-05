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
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.node.BreakNode;
import io.ballerina.flowmodelgenerator.core.model.node.CallNode;
import io.ballerina.flowmodelgenerator.core.model.node.ContinueNode;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandlerNode;
import io.ballerina.flowmodelgenerator.core.model.node.FailNode;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.IfNode;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.WhileNode;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingTreeModifier;
import org.ballerinalang.formatter.core.options.FormattingOptions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a node in the flow model.
 *
 * @since 1.4.0
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
        if (nodeProperties == null || !nodeProperties.isEmpty()) {
            this.nodeProperties = nodeProperties;
        }
    }

    protected FlowNode setCommonFields(LineRange lineRange, boolean returning, List<Branch> branches, int flags) {
        this.id = String.valueOf(Objects.hash(lineRange));
        this.lineRange = lineRange;
        this.returning = returning;
        this.branches = branches;
        this.flags = flags;
        return this;
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

    protected Expression getBranchProperty(Branch branch, String key) {
        return branch.properties() != null ? branch.properties().get(key) : null;
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

    public static final int NODE_FLAG_CHECKED = 1 << 0;
    public static final int NODE_FLAG_CHECKPANIC = 1 << 1;
    public static final int NODE_FLAG_FINAL = 1 << 2;
    public static final int NODE_FLAG_REMOTE = 1 << 10;
    public static final int NODE_FLAG_RESOURCE = 1 << 11;

    public enum Kind {
        EVENT_HTTP_API,
        IF,
        HTTP_API_GET_CALL,
        HTTP_API_POST_CALL,
        RETURN,
        EXPRESSION,
        ERROR_HANDLER,
        WHILE,
        CONTINUE,
        BREAK,
        FAIL
    }

    /**
     * Represents a builder for the flow node.
     *
     * @since 1.4.0
     */
    public static final class NodeBuilder {

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

        public NodeBuilder branch(Branch branch) {
            this.branches.add(branch);
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
            return node.setCommonFields(lineRange, returning, outBranches, flags);
        }
    }

    /**
     * Represents a builder for the node properties of a flow node. Each concrete flow node override this class to build
     * its properties.
     *
     * @since 1.4.0
     */
    public abstract static class NodePropertiesBuilder {

        public static final String VARIABLE_LABEL = "Variable";
        public static final String VARIABLE_KEY = "variable";
        public static final String VARIABLE_DOC = "Result Variable";

        public static final String EXPRESSION_RHS_LABEL = "Expression";
        public static final String EXPRESSION_RHS_KEY = "expression";
        public static final String EXPRESSION_RHS_DOC = "Expression";

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
            CommonUtils.getTypeSymbol(semanticModel, node).ifPresent(expressionBuilder::type);
            expressionBuilder
                    .label(VARIABLE_LABEL)
                    .value(CommonUtils.getVariableName(node))
                    .editable()
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .documentation(VARIABLE_DOC);

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

    /**
     * Represents a builder to generate a Ballerina source code.
     *
     * @since 1.4.0
     */
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
     * @since 1.4.0
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
                case ERROR_HANDLER -> context.deserialize(jsonObject, ErrorHandlerNode.class);
                case WHILE -> context.deserialize(jsonObject, WhileNode.class);
                case CONTINUE -> context.deserialize(jsonObject, ContinueNode.class);
                case BREAK -> context.deserialize(jsonObject, BreakNode.class);
                case FAIL -> context.deserialize(jsonObject, FailNode.class);
                case HTTP_API_GET_CALL, HTTP_API_POST_CALL -> context.deserialize(jsonObject, CallNode.class);
            };
        }
    }
}
