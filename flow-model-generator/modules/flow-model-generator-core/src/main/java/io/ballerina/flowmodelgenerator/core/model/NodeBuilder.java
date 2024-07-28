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
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Commit;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.HttpApiEvent;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.Lock;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Rollback;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.Transaction;
import io.ballerina.flowmodelgenerator.core.model.node.While;

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

    private static final Map<FlowNode.Kind, Supplier<? extends NodeBuilder>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(FlowNode.Kind.IF, If::new);
        put(FlowNode.Kind.RETURN, Return::new);
        put(FlowNode.Kind.EXPRESSION, DefaultExpression::new);
        put(FlowNode.Kind.ERROR_HANDLER, ErrorHandler::new);
        put(FlowNode.Kind.WHILE, While::new);
        put(FlowNode.Kind.CONTINUE, Continue::new);
        put(FlowNode.Kind.BREAK, Break::new);
        put(FlowNode.Kind.PANIC, Panic::new);
        put(FlowNode.Kind.EVENT_HTTP_API, HttpApiEvent::new);
        put(FlowNode.Kind.ACTION_CALL, ActionCall::new);
        put(FlowNode.Kind.START, Start::new);
        put(FlowNode.Kind.TRANSACTION, Transaction::new);
        put(FlowNode.Kind.LOCK, Lock::new);
        put(FlowNode.Kind.FAIL, Fail::new);
        put(FlowNode.Kind.COMMIT, Commit::new);
        put(FlowNode.Kind.ROLLBACK, Rollback::new);
    }};

    public static NodeBuilder getNodeFromKind(FlowNode.Kind kind) {
        return CONSTRUCTOR_MAP.getOrDefault(kind, DefaultExpression::new).get();
    }

    public NodeBuilder setConstData() {
        this.setConcreteConstData();
        return this;
    }

    public abstract void setConcreteConstData();

    public NodeBuilder setTemplateData(Codedata codedata) {
        setConcreteTemplateData(codedata);
        return this;
    }

    public abstract void setConcreteTemplateData(Codedata codedata);

    public abstract String toSource(FlowNode flowNode);

    public NodeBuilder() {
        this.branches = new ArrayList<>();
        this.flags = 0;
    }

    public NodeBuilder semanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
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
            this.propertiesBuilder = new PropertiesBuilder<>(semanticModel, this);
        }
        return this.propertiesBuilder;
    }

    public FlowNode build() {
        this.setConstData();
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

        public PropertiesBuilder(SemanticModel semanticModel, T parentBuilder) {
            super(parentBuilder);
            this.nodeProperties = new LinkedHashMap<>();
            this.propertyBuilder = Property.Builder.getInstance();
            this.semanticModel = semanticModel;
        }

        public PropertiesBuilder<T> variable(Node node) {
            if (node == null) {
                return this;
            }
            CommonUtils.getTypeSymbol(semanticModel, node).ifPresent(propertyBuilder::type);
            propertyBuilder
                    .metadata()
                    .label(Property.VARIABLE_LABEL)
                    .description(Property.VARIABLE_DOC)
                    .stepOut()
                    .value(CommonUtils.getVariableName(node))
                    .editable();

            addProperty(Property.VARIABLE_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(propertyBuilder::type);
            Property property = propertyBuilder
                    .metadata()
                    .label(Property.EXPRESSION_LABEL)
                    .description(Property.EXPRESSION_DOC)
                    .stepOut()
                    .editable()
                    .value(expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expressionNode).expression().toString() : expressionNode.toString())
                    .build();
            addProperty(Property.EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> callExpression(ExpressionNode expressionNode, ExpressionAttributes.Info info) {
            Property client = Property.Builder.getInstance()
                    .metadata()
                    .label(info.label())
                    .description(info.documentation())
                    .stepOut()
                    .type(info.type())
                    .value(expressionNode.toString())
                    .editable()
                    .build();
            addProperty(info.key(), client);
            return this;
        }

        public PropertiesBuilder<T> functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
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

            propertyBuilder = Property.Builder.getInstance();
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
                    propertyBuilder
                            .metadata()
                            .label(info.label())
                            .description(info.documentation())
                            .stepOut()
                            .editable()
                            .optional(parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE);

                    if (paramValue != null) {
                        propertyBuilder.value(paramValue.toSourceCode());
                    }

                    String staticType = info.type();
                    Optional<TypeSymbol> valueType =
                            paramValue != null ? semanticModel.typeOf(paramValue) : Optional.empty();

                    if (info.dynamicType() && valueType.isPresent()) {
                        // Obtain the type from the value if the dynamic type is set
                        propertyBuilder.type(valueType.get());
                    } else if (staticType != null) {
                        // Set the static type
                        propertyBuilder.type(staticType);
                    } else {
                        // Set the type of the symbol if none of types were found
                        propertyBuilder.type(parameterSymbol.typeDescriptor());
                    }

                    addProperty(parameterName, propertyBuilder.build());
                }
            }
            return this;
        }

        public PropertiesBuilder<T> resourceSymbol(ResourceMethodSymbol resourceMethodSymbol) {
            propertyBuilder
                    .metadata()
                    .label(EVENT_HTTP_API_METHOD)
                    .description(EVENT_HTTP_API_METHOD_DOC)
                    .stepOut()
                    .editable();
            resourceMethodSymbol.getName().ifPresent(name -> propertyBuilder.value(name));
            addProperty(EVENT_HTTP_API_METHOD_KEY, propertyBuilder.build());

            propertyBuilder
                    .metadata()
                    .label(EVENT_HTTP_API_PATH)
                    .description(EVENT_HTTP_API_PATH_DOC)
                    .stepOut()
                    .editable()
                    .value(resourceMethodSymbol.resourcePath().signature());
            addProperty(EVENT_HTTP_API_PATH_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> condition(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(propertyBuilder::type);
            Property condition = propertyBuilder
                    .metadata()
                    .label(Property.CONDITION_LABEL)
                    .description(Property.CONDITION_DOC)
                    .stepOut()
                    .value(expressionNode.toSourceCode())
                    .editable()
                    .build();
            addProperty(Property.CONDITION_KEY, condition);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode, String expressionDoc) {
            semanticModel.typeOf(expressionNode).ifPresent(propertyBuilder::type);
            Property property = propertyBuilder
                    .metadata()
                    .label(Property.EXPRESSION_DOC)
                    .description(expressionDoc)
                    .stepOut()
                    .value(expressionNode.toSourceCode())
                    .editable()
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
                    .value(node == null ? "" : node.toSourceCode())
                    .editable()
                    .build();
            addProperty(DefaultExpression.STATEMENT_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> ignore() {
            Property property = propertyBuilder
                    .metadata()
                    .label(Property.IGNORE_LABEL)
                    .description(Property.IGNORE_DOC)
                    .stepOut()
                    .value("true")
                    .editable()
                    .build();
            addProperty(Property.IGNORE_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> onErrorVariable(TypedBindingPatternNode typedBindingPatternNode) {
            BindingPatternNode bindingPatternNode = typedBindingPatternNode.bindingPattern();
            Property value = propertyBuilder
                    .metadata()
                    .label(Property.ON_ERROR_VARIABLE_LABEL)
                    .description(Property.ON_ERROR_VARIABLE_DOC)
                    .stepOut()
                    .value(bindingPatternNode.toString())
                    .editable()
                    .build();
            addProperty(Property.ON_ERROR_VARIABLE_KEY, value);

            CommonUtils.getTypeSymbol(semanticModel, typedBindingPatternNode)
                    .ifPresent(typeSymbol -> propertyBuilder.value(CommonUtils.getTypeSignature(typeSymbol)));
            Property type = propertyBuilder
                    .metadata()
                    .label(Property.ON_ERROR_TYPE_LABEL)
                    .description(Property.ON_ERROR_TYPE_DOC)
                    .stepOut()
                    .editable()
                    .build();
            addProperty(Property.ON_ERROR_TYPE_KEY, type);

            return this;
        }

        public PropertiesBuilder<T> defaultOnErrorVariable() {
            Property value = propertyBuilder
                    .metadata()
                    .label(Property.ON_ERROR_VARIABLE_LABEL)
                    .description(Property.ON_ERROR_VARIABLE_DOC)
                    .stepOut()
                    .value("err")
                    .editable()
                    .build();
            addProperty(Property.ON_ERROR_VARIABLE_KEY, value);

            Property type = propertyBuilder
                    .metadata()
                    .label(Property.ON_ERROR_TYPE_LABEL)
                    .description(Property.ON_ERROR_TYPE_DOC)
                    .stepOut()
                    .value("error")
                    .editable()
                    .build();
            addProperty(Property.ON_ERROR_TYPE_KEY, type);

            return this;
        }

        public PropertiesBuilder<T> defaultExpression(String doc) {
            Property property = propertyBuilder
                    .metadata()
                    .label(Property.EXPRESSION_LABEL)
                    .description(doc)
                    .stepOut()
                    .value("")
                    .editable()
                    .build();
            addProperty(Property.EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> defaultVariable() {
            propertyBuilder
                    .metadata()
                    .label(Property.VARIABLE_LABEL)
                    .description(Property.VARIABLE_DOC)
                    .stepOut()
                    .value("item")
                    .editable()
                    .optional(true);

            addProperty(Property.VARIABLE_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder<T> defaultCondition(String doc) {
            Property property = propertyBuilder
                    .metadata()
                    .label(Property.CONDITION_LABEL)
                    .description(doc)
                    .stepOut()
                    .value("true")
                    .editable()
                    .build();
            addProperty(Property.CONDITION_KEY, property);
            return this;
        }

        public PropertiesBuilder<T> defaultExpression(ExpressionAttributes.Info info) {
            Property property = propertyBuilder
                    .metadata()
                    .label(info.label())
                    .description(info.documentation())
                    .stepOut()
                    .value("")
                    .type(info.type())
                    .editable()
                    .build();
            addProperty(info.key(), property);
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
