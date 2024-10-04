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
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Assign;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryData;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Comment;
import io.ballerina.flowmodelgenerator.core.model.node.Commit;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapper;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.Foreach;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionCall;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayload;
import io.ballerina.flowmodelgenerator.core.model.node.Lock;
import io.ballerina.flowmodelgenerator.core.model.node.Match;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnection;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Retry;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Rollback;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.Stop;
import io.ballerina.flowmodelgenerator.core.model.node.Transaction;
import io.ballerina.flowmodelgenerator.core.model.node.While;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayload;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
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

import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.FUNCTION_NAME_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.FUNCTION_NAME_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.FUNCTION_NAME_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.INPUTS_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.INPUTS_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.INPUTS_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.OUTPUT_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.OUTPUT_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.OUTPUT_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_METHOD;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_METHOD_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_METHOD_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_PATH;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_PATH_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent.EVENT_HTTP_API_PATH_KEY;

/**
 * Represents a builder for the flow node.
 *
 * @since 1.4.0
 */
public abstract class NodeBuilder {

    protected List<Branch> branches;
    protected Metadata.Builder<NodeBuilder> metadataBuilder;
    protected Codedata.Builder<NodeBuilder> codedataBuilder;
    protected PropertiesBuilder<NodeBuilder> propertiesBuilder;
    protected int flags;
    protected boolean returning;
    protected SemanticModel semanticModel;
    protected FlowNode cachedFlowNode;
    protected String defaultModuleName;

    private static final Map<NodeKind, Supplier<? extends NodeBuilder>> CONSTRUCTOR_MAP = new HashMap<>() {
        {
            put(NodeKind.IF, If::new);
            put(NodeKind.RETURN, Return::new);
            put(NodeKind.EXPRESSION, DefaultExpression::new);
            put(NodeKind.ERROR_HANDLER, ErrorHandler::new);
            put(NodeKind.WHILE, While::new);
            put(NodeKind.CONTINUE, Continue::new);
            put(NodeKind.BREAK, Break::new);
            put(NodeKind.PANIC, Panic::new);
            put(NodeKind.EVENT_HTTP_API, HttpApiEvent::new);
            put(NodeKind.ACTION_CALL, ActionCall::new);
            put(NodeKind.NEW_CONNECTION, NewConnection::new);
            put(NodeKind.START, Start::new);
            put(NodeKind.TRANSACTION, Transaction::new);
            put(NodeKind.RETRY, Retry::new);
            put(NodeKind.LOCK, Lock::new);
            put(NodeKind.FAIL, Fail::new);
            put(NodeKind.COMMIT, Commit::new);
            put(NodeKind.ROLLBACK, Rollback::new);
            put(NodeKind.XML_PAYLOAD, XmlPayload::new);
            put(NodeKind.JSON_PAYLOAD, JsonPayload::new);
            put(NodeKind.BINARY_DATA, BinaryData::new);
            put(NodeKind.STOP, Stop::new);
            put(NodeKind.FUNCTION_CALL, FunctionCall::new);
            put(NodeKind.FOREACH, Foreach::new);
            put(NodeKind.DATA_MAPPER, DataMapper::new);
            put(NodeKind.ASSIGN, Assign::new);
            put(NodeKind.COMMENT, Comment::new);
            put(NodeKind.MATCH, Match::new);
        }};


    public static NodeBuilder getNodeFromKind(NodeKind kind) {
        return CONSTRUCTOR_MAP.getOrDefault(kind, DefaultExpression::new).get();
    }

    public NodeBuilder setConstData() {
        this.setConcreteConstData();
        return this;
    }

    public abstract void setConcreteConstData();

    public NodeBuilder setTemplateData(TemplateContext context) {
        setConcreteTemplateData(context);
        return this;
    }

    public abstract void setConcreteTemplateData(TemplateContext context);

    public abstract Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder);

    public NodeBuilder() {
        this.branches = new ArrayList<>();
        this.flags = 0;
    }

    public NodeBuilder semanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        return this;
    }

    public NodeBuilder defaultModuleName(String defaultModuleName) {
        this.defaultModuleName = defaultModuleName;
        return this;
    }

    public NodeBuilder returning() {
        this.returning = true;
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

    public Metadata.Builder<NodeBuilder> metadata() {
        if (this.metadataBuilder == null) {
            this.metadataBuilder = new Metadata.Builder<>(this);
        }
        return this.metadataBuilder;
    }

    public Codedata.Builder<NodeBuilder> codedata() {
        if (this.codedataBuilder == null) {
            this.codedataBuilder = new Codedata.Builder<>(this);
        }
        return this.codedataBuilder;
    }

    public PropertiesBuilder<NodeBuilder> properties() {
        if (this.propertiesBuilder == null) {
            this.propertiesBuilder = new PropertiesBuilder<>(semanticModel, defaultModuleName, this);
        }
        return this.propertiesBuilder;
    }

    public FlowNode build() {
        this.setConstData();

        // Check if there is a pre-built node
        if (cachedFlowNode != null) {
            return cachedFlowNode;
        }

        Codedata codedata = codedataBuilder == null ? null : codedataBuilder.build();
        return new FlowNode(
                String.valueOf(Objects.hash(codedata != null ? codedata.lineRange() : null)),
                metadataBuilder == null ? null : metadataBuilder.build(),
                codedata,
                returning,
                branches.isEmpty() ? null : branches,
                propertiesBuilder == null ? null : propertiesBuilder.build(),
                flags
        );
    }

    public AvailableNode buildAvailableNode() {
        this.setConcreteConstData();
        return new AvailableNode(metadataBuilder == null ? null : metadataBuilder.build(),
                codedataBuilder == null ? null : codedataBuilder.build(), true);
    }

    public record TemplateContext(WorkspaceManager workspaceManager, Path filePath, LinePosition position,
                                  Codedata codedata) {

    }

    /**
     * Represents a builder for the node properties of a flow node.
     *
     * @param <T> Parent builder type
     * @since 1.4.0
     */
    public static class PropertiesBuilder<T> extends FacetedBuilder<T> {

        private final Map<String, Property> nodeProperties;
        private final SemanticModel semanticModel;
        protected Property.Builder propertyBuilder;
        private final String defaultModuleName;

        public PropertiesBuilder(SemanticModel semanticModel, String defaultModuleName, T parentBuilder) {
            super(parentBuilder);
            this.nodeProperties = new LinkedHashMap<>();
            this.propertyBuilder = Property.Builder.getInstance();
            this.semanticModel = semanticModel;
            this.defaultModuleName = defaultModuleName;
        }

        public PropertiesBuilder<T> variable(Node node) {
            if (node == null) {
                return this;
            }
            propertyBuilder
                    .metadata()
                        .label(Property.VARIABLE_LABEL)
                        .description(Property.VARIABLE_DOC)
                        .stepOut()
                    .value(CommonUtils.getVariableName(node))
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();

            addProperty(Property.VARIABLE_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> type(Node node) {
            propertyBuilder
                    .metadata()
                        .label(Property.DATA_TYPE_LABEL)
                        .description(Property.DATA_TYPE_DOC)
                        .stepOut()
                    .value(CommonUtils.getVariableName(node))
                    .type(Property.ValueType.TYPE)
                    .editable();

            addProperty(Property.DATA_TYPE_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> type(String typeName) {
            propertyBuilder
                    .metadata()
                        .label(Property.DATA_TYPE_LABEL)
                        .description(Property.DATA_TYPE_DOC)
                        .stepOut()
                    .value(typeName)
                    .type(Property.ValueType.TYPE)
                    .editable();

            addProperty(Property.DATA_TYPE_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> dataVariable(Node node) {
            data(node);

            propertyBuilder
                    .metadata()
                        .label(Property.DATA_TYPE_LABEL)
                        .description(Property.DATA_TYPE_DOC)
                        .stepOut()
                    .type(Property.ValueType.TYPE)
                    .editable();

            if (node == null) {
                propertyBuilder.value("var");
            } else {
                Optional<TypeSymbol> optTypeSymbol = CommonUtils.getTypeSymbol(semanticModel, node);
                optTypeSymbol.ifPresent(typeSymbol -> propertyBuilder.value(
                        CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, defaultModuleName)));
            }

            addProperty(Property.DATA_TYPE_KEY, propertyBuilder.build());

            return this;
        }

        public PropertiesBuilder<T> payload(Node node, String type) {
            data(node);

            propertyBuilder
                    .metadata()
                        .label(Property.DATA_TYPE_LABEL)
                        .description(Property.DATA_TYPE_DOC)
                        .stepOut()
                    .type(Property.ValueType.TYPE)
                    .editable();

            if (node == null) {
                propertyBuilder.value(type);
            } else {
                Optional<TypeSymbol> optTypeSymbol = CommonUtils.getTypeSymbol(semanticModel, node);
                optTypeSymbol.ifPresent(typeSymbol -> propertyBuilder.value(
                        CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, defaultModuleName)));
            }
            addProperty(Property.DATA_TYPE_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> data(Node node) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.DATA_VARIABLE_LABEL)
                        .description(Property.DATA_VARIABLE_DOC)
                        .stepOut()
                    .value(node == null ? "item" : CommonUtils.getVariableName(node))
                    .type(Property.ValueType.IDENTIFIER)
                    .editable()
                    .build();
            addProperty(Property.DATA_VARIABLE_KEY, property);

            return this;
        }

        public PropertiesBuilder<T> patterns(NodeList<? extends Node> node) {
            List<Property> properties = new ArrayList<>();
            for (Node patternNode : node) {
                Property property = propertyBuilder
                        .metadata()
                            .label(Property.PATTERN_LABEL)
                            .description(Property.PATTERN_DOC)
                            .stepOut()
                        .value(patternNode.toSourceCode().strip())
                        .type(Property.ValueType.EXPRESSION)
                        .editable()
                        .build();
                properties.add(property);
            }

            Property property = propertyBuilder
                    .metadata()
                        .label(Property.PATTERNS_LABEL)
                        .description(Property.PATTERNS_DOC)
                        .stepOut()
                    .value(properties)
                    .type(Property.ValueType.SINGLE_SELECT)
                    .editable()
                    .build();
            addProperty(Property.PATTERNS_KEY, property);

            return this;
        }

        public PropertiesBuilder<T> callExpression(ExpressionNode expressionNode, String key,
                                                   Property propertyTemplate) {
            Property client = Property.Builder.getInstance()
                    .metadata()
                        .label(propertyTemplate.metadata().label())
                        .description(propertyTemplate.metadata().description())
                        .stepOut()
                    .type(Property.ValueType.EXPRESSION)
                    .value(expressionNode.toString())
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(key, client);
            return this;
        }

        // TODO: Think how we can reuse this logic with the functionArguments method
        public PropertiesBuilder<T> inputs(SeparatedNodeList<FunctionArgumentNode> arguments,
                                           List<ParameterSymbol> parameterSymbols) {
            final Map<String, Node> namedArgValueMap = new HashMap<>();
            final Queue<Node> positionalArgs = new LinkedList<>();

            if (arguments != null) {
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
            }

            propertyBuilder = Property.Builder.getInstance();
            int numParams = parameterSymbols.size();
            int numPositionalArgs = positionalArgs.size();

            List<String> inputs = new ArrayList<>();
            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                Optional<String> name = parameterSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }
                String parameterName = name.get();
                Node paramValue = i < numPositionalArgs ? positionalArgs.poll() : namedArgValueMap.get(parameterName);

                String type = CommonUtils.getTypeSignature(semanticModel, parameterSymbol.typeDescriptor(), false,
                        defaultModuleName);
                String variableName = CommonUtils.getVariableName(paramValue);
                inputs.add(type + " " + variableName);
            }

            propertyBuilder
                    .metadata()
                        .label(INPUTS_LABEL)
                        .description(INPUTS_DOC)
                        .stepOut()
                    .type(Property.ValueType.MULTIPLE_SELECT)
                    .value(inputs)
                    .editable();

            addProperty(INPUTS_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> output(Node node) {
            propertyBuilder
                    .metadata()
                        .label(OUTPUT_LABEL)
                        .description(OUTPUT_DOC)
                        .stepOut()
                    .type(Property.ValueType.SINGLE_SELECT)
                    .editable();

            Optional<TypeSymbol> optTypeSymbol = CommonUtils.getTypeSymbol(semanticModel, node);
            optTypeSymbol.ifPresent(
                    typeSymbol -> propertyBuilder.value(
                            CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, defaultModuleName)));

            addProperty(OUTPUT_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
                                                      List<ParameterSymbol> parameterSymbols,
                                                      Map<String, Property> properties) {
            final Map<String, Node> namedArgValueMap = new HashMap<>();
            final Queue<Node> positionalArgs = new LinkedList<>();

            if (arguments != null) {
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
            }

            propertyBuilder = Property.Builder.getInstance();
            int numParams = parameterSymbols.size();
            int numPositionalArgs = positionalArgs.size();

            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                Optional<String> name = parameterSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }

                String parameterName = name.get().startsWith("'") ? name.get().substring(1) : name.get();
                Node paramValue = i < numPositionalArgs ? positionalArgs.poll() : namedArgValueMap.get(parameterName);

                Property propertyTemplate = properties.get(parameterName);
                if (propertyTemplate != null) {
                    propertyBuilder
                            .metadata()
                                .label(propertyTemplate.metadata().label())
                                .description(propertyTemplate.metadata().description())
                                .stepOut()
                            .type(Property.ValueType.EXPRESSION)
                            .editable()
                            .optional(parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE);

                    if (paramValue != null) {
                        propertyBuilder.value(paramValue.toSourceCode());
                    }

                    addProperty(parameterName, propertyBuilder.build());
                }
            }
            return this;
        }

        public PropertiesBuilder<T> functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
                                                      List<ParameterSymbol> parameterSymbols) {
            final Map<String, Node> namedArgValueMap = new HashMap<>();
            final Queue<Node> positionalArgs = new LinkedList<>();

            if (arguments != null) {
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
            }

            propertyBuilder = Property.Builder.getInstance();
            int numParams = parameterSymbols.size();
            int numPositionalArgs = positionalArgs.size();

            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);
                Optional<String> name = parameterSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }

                String parameterName = name.get().startsWith("'") ? name.get().substring(1) : name.get();
                Node paramValue = i < numPositionalArgs ? positionalArgs.poll() : namedArgValueMap.get(parameterName);

                propertyBuilder
                        .metadata()
                            .label(parameterName)
                            .stepOut()
                        .type(Property.ValueType.EXPRESSION)
                        .editable()
                        .optional(parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE);

                if (paramValue != null) {
                    propertyBuilder.value(paramValue.toSourceCode());
                }

                addProperty(parameterName, propertyBuilder.build());

            }
            return this;
        }

        public PropertiesBuilder<T> resourceSymbol(ResourceMethodSymbol resourceMethodSymbol) {
            propertyBuilder
                    .metadata()
                        .label(EVENT_HTTP_API_METHOD)
                        .description(EVENT_HTTP_API_METHOD_DOC)
                        .stepOut()
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();
            resourceMethodSymbol.getName().ifPresent(name -> propertyBuilder.value(name));
            addProperty(EVENT_HTTP_API_METHOD_KEY, propertyBuilder.build());

            propertyBuilder
                    .metadata()
                        .label(EVENT_HTTP_API_PATH)
                        .description(EVENT_HTTP_API_PATH_DOC)
                        .stepOut()
                    .editable()
                    .type(Property.ValueType.STRING)
                    .value(resourceMethodSymbol.resourcePath().signature());
            addProperty(EVENT_HTTP_API_PATH_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> condition(ExpressionNode expressionNode) {
            Property condition = propertyBuilder
                    .metadata()
                        .label(Property.CONDITION_LABEL)
                        .description(Property.CONDITION_DOC)
                        .stepOut()
                    .value(expressionNode == null ? "true" : expressionNode.toSourceCode())
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(Property.CONDITION_KEY, condition);
            return this;
        }

        public PropertiesBuilder<T> retryCount(int retryCount) {
            return retryCount(retryCount, false);
        }

        public PropertiesBuilder<T> retryCount(int retryCount, boolean optional) {
            Property property = propertyBuilder
                    .metadata()
                    .label(Property.RETRY_COUNT_LABEL)
                    .description(Property.RETRY_COUNT_DOC)
                    .stepOut()
                    .value(String.valueOf(retryCount))
                    .type(Property.ValueType.EXPRESSION)
                    .optional(optional)
                    .editable()
                    .build();
            addProperty(Property.RETRY_COUNT_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> expression(String expr, String expressionDoc) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_DOC)
                        .description(expressionDoc)
                        .stepOut()
                    .value(expr)
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(Property.EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode, String expressionDoc) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_DOC)
                        .description(expressionDoc)
                        .stepOut()
                    .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(Property.EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode, String key, String expressionDoc) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_DOC)
                        .description(expressionDoc)
                        .stepOut()
                    .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(key, property);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_LABEL)
                        .description(Property.EXPRESSION_DOC)
                        .stepOut()
                    .editable()
                    .value(expressionNode == null ? "" : expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expressionNode).expression().toString() : expressionNode.toString())
                    .type(Property.ValueType.EXPRESSION)
                    .build();
            addProperty(Property.EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> statement(Node node) {
            Property property = propertyBuilder
                    .metadata()
                        .label(DefaultExpression.STATEMENT_LABEL)
                        .description(DefaultExpression.STATEMENT_DOC)
                        .stepOut()
                    .value(node == null ? "" : node.toSourceCode().strip())
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(DefaultExpression.STATEMENT_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> ignore(boolean ignore) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.IGNORE_LABEL)
                        .description(Property.IGNORE_DOC)
                        .stepOut()
                    .value(String.valueOf(ignore))
                    .type(Property.ValueType.EXPRESSION)
                    .editable()
                    .build();
            addProperty(Property.IGNORE_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> comment(String comment) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.COMMENT_LABEL)
                        .description(Property.COMMENT_DOC)
                        .stepOut()
                    .value(comment)
                    .type(Property.ValueType.STRING)
                    .editable()
                    .build();
            addProperty(Property.COMMENT_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> onErrorVariable(TypedBindingPatternNode typedBindingPatternNode) {
            Property variable = propertyBuilder
                    .metadata()
                        .label(Property.ON_ERROR_VARIABLE_LABEL)
                        .description(Property.ON_ERROR_VARIABLE_DOC)
                        .stepOut()
                    .value(typedBindingPatternNode == null ? "err" :
                            typedBindingPatternNode.bindingPattern().toString())
                    .type(Property.ValueType.IDENTIFIER)
                    .editable()
                    .build();
            addProperty(Property.ON_ERROR_VARIABLE_KEY, variable);

            if (typedBindingPatternNode == null) {
                propertyBuilder.value("error");
            } else {
                CommonUtils.getTypeSymbol(semanticModel, typedBindingPatternNode)
                        .ifPresent(typeSymbol -> propertyBuilder.value(
                                CommonUtils.getTypeSignature(semanticModel, typeSymbol, false, defaultModuleName)));
            }
            Property type = propertyBuilder
                    .metadata()
                        .label(Property.ON_ERROR_TYPE_LABEL)
                        .description(Property.ON_ERROR_TYPE_DOC)
                        .stepOut()
                    .editable()
                    .type(Property.ValueType.TYPE)
                    .build();
            addProperty(Property.ON_ERROR_TYPE_KEY, type);

            return this;
        }

        public PropertiesBuilder<T> functionName(String functionName) {
            Property property = propertyBuilder
                    .metadata()
                        .label(FUNCTION_NAME_LABEL)
                        .description(FUNCTION_NAME_DOC)
                        .stepOut()
                    .type(Property.ValueType.IDENTIFIER)
                    .value(functionName)
                    .editable()
                    .build();

            addProperty(FUNCTION_NAME_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> custom(String key, String label, String description, Property.ValueType type,
                                           Object typeConstraint, String value, boolean optional) {
            Property property = propertyBuilder
                    .metadata()
                        .label(label)
                        .description(description)
                        .stepOut()
                    .type(type)
                    .typeConstraint(typeConstraint)
                    .value(value)
                    .editable()
                    .optional(optional)
                    .build();

            addProperty(key, property);
            return this;
        }

        public PropertiesBuilder<T> scope(String scope) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.SCOPE_LABEL)
                        .description(Property.SCOPE_DOC)
                        .stepOut()
                    .type(Property.ValueType.ENUM)
                    .value(scope)
                    .editable()
                    .build();
            addProperty(Property.SCOPE_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> view(LineRange lineRange) {
            Property property = propertyBuilder
                    .metadata()
                        .label(DataMapper.VIEW_LABEL)
                        .description(DataMapper.VIEW_DOC)
                        .stepOut()
                    .value(lineRange)
                    .type(Property.ValueType.VIEW)
                    .build();
            addProperty(DataMapper.VIEW_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> collection(Node expressionNode) {
            Property property = propertyBuilder
                    .metadata()
                        .label(Property.COLLECTION_LABEL)
                        .description(Property.COLLECTION_DOC)
                        .stepOut()
                    .editable()
                    .value(expressionNode == null ? "[]" : expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expressionNode).expression().toString() : expressionNode.toString())
                    .type(Property.ValueType.EXPRESSION)
                    .build();
            addProperty(Property.COLLECTION_KEY, property);
            return this;
        }

        public final void addProperty(String key, Property property) {
            if (property != null) {
                this.nodeProperties.put(key, property);
            }
        }

        public Map<String, Property> build() {
            return this.nodeProperties;
        }
    }
}
