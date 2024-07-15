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
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.Lock;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.Transaction;
import io.ballerina.flowmodelgenerator.core.model.node.While;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.formatter.core.FormattingTreeModifier;
import org.ballerinalang.formatter.core.options.FormattingOptions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Supplier;

import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_METHOD;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_METHOD_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_METHOD_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_PATH;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_PATH_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_PATH_KEY;

/**
 * Represents a node in the flow model.
 *
 * @since 1.4.0
 */
public abstract class FlowNode {

    protected String id;
    protected String label;
    protected String description;
    protected Kind kind;
    protected LineRange lineRange;
    protected boolean returning;
    protected List<Branch> branches;
    protected Map<String, Expression> nodeProperties;
    protected int flags;

    private static final Map<FlowNode.Kind, Supplier<? extends FlowNode>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(Kind.IF, If::new);
        put(Kind.RETURN, Return::new);
        put(Kind.EXPRESSION, DefaultExpression::new);
        put(Kind.ERROR_HANDLER, ErrorHandler::new);
        put(Kind.WHILE, While::new);
        put(Kind.CONTINUE, Continue::new);
        put(Kind.BREAK, Break::new);
        put(Kind.PANIC, Panic::new);
        put(Kind.EVENT_HTTP_API, HttpApiEvent::new);
        put(Kind.ACTION_CALL, ActionCall::new);
        put(Kind.START, Start::new);
        put(Kind.TRANSACTION, Transaction::new);
        put(Kind.LOCK, Lock::new);
        put(Kind.FAIL, Fail::new);
    }};

    public static FlowNode getNodeFromKind(Kind kind) {
        return CONSTRUCTOR_MAP.getOrDefault(kind, DefaultExpression::new).get();
    }

    protected FlowNode() {
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

    public AvailableNode extractAvailableNode() {
        this.setConstData();
        return new AvailableNode(kind.name(), label, description, null, true);
    }

    public abstract void setConstData();

    public abstract String toSource();

    public abstract void setTemplateData();

    public static final int NODE_FLAG_CHECKED = 1 << 0;
    public static final int NODE_FLAG_CHECKPANIC = 1 << 1;
    public static final int NODE_FLAG_FINAL = 1 << 2;
    public static final int NODE_FLAG_REMOTE = 1 << 10;
    public static final int NODE_FLAG_RESOURCE = 1 << 11;

    public enum Kind {
        EVENT_HTTP_API,
        IF,
        ACTION_CALL,
        RETURN,
        EXPRESSION,
        ERROR_HANDLER,
        WHILE,
        CONTINUE,
        BREAK,
        PANIC,
        START,
        TRANSACTION,
        LOCK,
        FAIL
    }

    /**
     * Represents a builder for the flow node.
     *
     * @since 1.4.0
     */
    public static final class NodeBuilder {

        private int flags;
        private final List<Branch> branches;
        private PropertiesBuilder propertiesBuilder;
        private final SemanticModel semanticModel;
        private final FlowNode flowNode;

        public <T extends FlowNode> NodeBuilder(SemanticModel semanticModel, Supplier<? extends FlowNode> constructor) {
            this.branches = new ArrayList<>();
            this.flags = 0;
            this.semanticModel = semanticModel;
            this.flowNode = constructor.get();
        }

        public NodeBuilder returning() {
            flowNode.returning = true;
            return this;
        }

        public NodeBuilder lineRange(Node node) {
            flowNode.lineRange = node.lineRange();
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

        public NodeBuilder kind(Kind kind) {
            flowNode.kind = kind;
            return this;
        }

        public NodeBuilder label(String label) {
            flowNode.label = label;
            return this;
        }

        public PropertiesBuilder properties() {
            if (this.propertiesBuilder == null) {
                this.propertiesBuilder = new PropertiesBuilder(semanticModel);
            }
            return this.propertiesBuilder;
        }

        public boolean isDefault() {
            return this.propertiesBuilder == null;
        }

        public FlowNode build() {
            flowNode.id = String.valueOf(Objects.hash(flowNode.lineRange));
            flowNode.nodeProperties = propertiesBuilder == null ? null : propertiesBuilder.build();
            flowNode.branches = branches.isEmpty() ? null : branches;
            flowNode.flags = flags;
            flowNode.setConstData();
            return flowNode;
        }
    }

    /**
     * Represents a builder for the node properties of a flow node. Each concrete flow node override this class to build
     * its properties.
     *
     * @since 1.4.0
     */
    public static class PropertiesBuilder {

        public static final String VARIABLE_LABEL = "Variable";
        public static final String VARIABLE_KEY = "variable";
        public static final String VARIABLE_DOC = "Result Variable";

        public static final String EXPRESSION_RHS_LABEL = "Expression";
        public static final String EXPRESSION_RHS_KEY = "expression";
        public static final String EXPRESSION_RHS_DOC = "Expression";

        public static final String CONDITION_LABEL = "Condition";
        public static final String CONDITION_KEY = "condition";
        public static final String CONDITION_DOC = "Boolean Condition";

        private final Map<String, Expression> nodeProperties;
        private final SemanticModel semanticModel;
        protected Expression.Builder expressionBuilder;

        public PropertiesBuilder(SemanticModel semanticModel) {
            this.nodeProperties = new LinkedHashMap<>();
            this.expressionBuilder = Expression.Builder.getInstance();
            this.semanticModel = semanticModel;
        }

        @SuppressWarnings("unchecked")
        public <T extends PropertiesBuilder> T variable(Node node) {
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

            addProperty(VARIABLE_KEY, expressionBuilder.build());
            return (T) this;
        }

        public PropertiesBuilder expression(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(expressionBuilder::type);
            Expression expression = expressionBuilder
                    .label(EXPRESSION_RHS_LABEL)
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .documentation(EXPRESSION_RHS_DOC)
                    .editable()
                    .value(expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expressionNode).expression().toString() : expressionNode.toString())
                    .build();
            addProperty(EXPRESSION_RHS_KEY, expression);
            return this;
        }

        public PropertiesBuilder callExpression(ExpressionNode expressionNode, ExpressionAttributes.Info info) {
            Expression client = Expression.Builder.getInstance()
                    .label(info.label())
                    .type(info.type())
                    .value(expressionNode.toString())
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .editable()
                    .documentation(info.documentation())
                    .build();
            addProperty(info.key(), client);
            return this;
        }

        public PropertiesBuilder functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
                                                   List<ParameterSymbol> parameterSymbols) {
            final Map<String, Node> namedArgValueMap = new HashMap<>();
            final Queue<Node> positionalArgs = new LinkedList<>();

            for (FunctionArgumentNode argument : arguments) {
                switch (argument.kind()) {
                    case NAMED_ARG -> {
                        NamedArgumentNode namedArgument = (NamedArgumentNode) argument;
                        namedArgValueMap.put(namedArgument.argumentName().name().text(),
                                namedArgument.expression());
                    }
                    case POSITIONAL_ARG -> positionalArgs.add(((PositionalArgumentNode) argument).expression());
                    default -> {
                        // Ignore the default case
                    }
                }
            }

            expressionBuilder = Expression.Builder.getInstance();
            int numParams = parameterSymbols.size();
            int numPositionalArgs = positionalArgs.size();

            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                Optional<String> name = parameterSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }
                String parameterName = name.get();
                Node paramValue = i < numPositionalArgs ? positionalArgs.poll() : namedArgValueMap.get(parameterName);

                ExpressionAttributes.Info info = ExpressionAttributes.get(parameterName);
                if (info != null) {
                    expressionBuilder
                            .label(info.label())
                            .documentation(info.documentation())
                            .typeKind(Expression.ExpressionTypeKind.BTYPE)
                            .editable()
                            .optional(parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE);

                    if (paramValue != null) {
                        expressionBuilder.value(paramValue.toSourceCode());
                    }

                    String staticType = info.type();
                    Optional<TypeSymbol> valueType =
                            paramValue != null ? semanticModel.typeOf(paramValue) : Optional.empty();

                    if (info.dynamicType() && valueType.isPresent()) {
                        // Obtain the type from the value if the dynamic type is set
                        expressionBuilder.type(valueType.get());
                    } else if (staticType != null) {
                        // Set the static type
                        expressionBuilder.type(staticType);
                    } else {
                        // Set the type of the symbol if none of types were found
                        expressionBuilder.type(parameterSymbol.typeDescriptor());
                    }

                    addProperty(parameterName, expressionBuilder.build());
                }
            }
            return this;
        }

        public PropertiesBuilder resourceSymbol(ResourceMethodSymbol resourceMethodSymbol) {
            expressionBuilder
                    .label(EVENT_HTTP_API_METHOD)
                    .typeKind(Expression.ExpressionTypeKind.IDENTIFIER)
                    .editable()
                    .documentation(EVENT_HTTP_API_METHOD_DOC);
            resourceMethodSymbol.getName().ifPresent(name -> expressionBuilder.value(name));
            addProperty(EVENT_HTTP_API_METHOD_KEY, expressionBuilder.build());

            expressionBuilder
                    .label(EVENT_HTTP_API_PATH)
                    .typeKind(Expression.ExpressionTypeKind.URI_PATH)
                    .editable()
                    .documentation(EVENT_HTTP_API_PATH_DOC)
                    .value(resourceMethodSymbol.resourcePath().signature());
            addProperty(EVENT_HTTP_API_PATH_KEY, expressionBuilder.build());
            return this;
        }

        public PropertiesBuilder setConditionExpression(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(expressionBuilder::type);
            Expression condition = expressionBuilder
                    .label(CONDITION_LABEL)
                    .value(expressionNode.toSourceCode())
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .documentation(CONDITION_DOC)
                    .editable()
                    .build();
            addProperty(CONDITION_KEY, condition);
            return this;
        }

        public PropertiesBuilder setExpressionNode(ExpressionNode expressionNode, String expressionDoc) {
            semanticModel.typeOf(expressionNode).ifPresent(expressionBuilder::type);
            Expression expression = expressionBuilder
                    .label(EXPRESSION_RHS_DOC)
                    .value(expressionNode.toSourceCode())
                    .documentation(expressionDoc)
                    .typeKind(Expression.ExpressionTypeKind.BTYPE)
                    .editable()
                    .build();
            addProperty(EXPRESSION_RHS_KEY, expression);
            return this;
        }

        public final void addProperty(String key, Expression expression) {
            if (expression != null) {
                this.nodeProperties.put(key, expression);
            }
        }

        public Map<String, Expression> build() {
            return this.nodeProperties;
        }
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
                case IF -> context.deserialize(jsonObject, If.class);
                case EVENT_HTTP_API -> context.deserialize(jsonObject, HttpApiEvent.class);
                case ACTION_CALL -> context.deserialize(jsonObject, ActionCall.class);
                case RETURN -> context.deserialize(jsonObject, Return.class);
                case ERROR_HANDLER -> context.deserialize(jsonObject, ErrorHandler.class);
                case WHILE -> context.deserialize(jsonObject, While.class);
                case CONTINUE -> context.deserialize(jsonObject, Continue.class);
                case BREAK -> context.deserialize(jsonObject, Break.class);
                case PANIC -> context.deserialize(jsonObject, Panic.class);
                case START -> context.deserialize(jsonObject, Start.class);
                case FAIL -> context.deserialize(jsonObject, Fail.class);
                case TRANSACTION -> context.deserialize(jsonObject, Transaction.class);
                case LOCK -> context.deserialize(jsonObject, Lock.class);
            };
        }
    }
}
