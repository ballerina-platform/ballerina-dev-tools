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

    protected String label;
    protected FlowNode.Kind kind;
    protected String description;
    protected String org;
    protected String module;
    protected String object;
    protected String symbol;
    protected List<Branch> branches;
    protected PropertiesBuilder propertiesBuilder;
    protected int flags;
    protected boolean returning;
    protected LineRange lineRange;
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
    }};

    public static NodeBuilder getNodeFromKind(FlowNode.Kind kind) {
        return CONSTRUCTOR_MAP.getOrDefault(kind, DefaultExpression::new).get();
    }

    public NodeBuilder setConstData() {
        this.setConcreteConstData();
        return this;
    }

    public abstract void setConcreteConstData();

    public NodeBuilder setTemplateData() {
//        this.id = "0";
        setConcreteTemplateData();
        return this;
    }

    public abstract void setConcreteTemplateData();

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

    public NodeBuilder lineRange(Node node) {
        this.lineRange = node.lineRange();
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

    public NodeBuilder kind(FlowNode.Kind kind) {
        this.kind = kind;
        return this;
    }

    public NodeBuilder label(String label) {
        this.label = label;
        return this;
    }

    public NodeBuilder description(String description) {
        this.description = description;
        return this;
    }

    public NodeBuilder org(String org) {
        this.org = org;
        return this;
    }

    public NodeBuilder module(String module) {
        this.module = module;
        return this;
    }

    public NodeBuilder object(String object) {
        this.object = object;
        return this;
    }

    public NodeBuilder symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public PropertiesBuilder properties() {
        if (this.propertiesBuilder == null) {
            this.propertiesBuilder = new PropertiesBuilder(semanticModel);
        }
        return this.propertiesBuilder;
    }

    public FlowNode build() {
        this.setConstData();
        return new FlowNode(
                String.valueOf(Objects.hash(lineRange)),
                new Metadata(label, description, null),
                new Codedata(kind, null, module, null, symbol),
                lineRange,
                returning,
                branches.isEmpty() ? null : branches,
                propertiesBuilder == null ? null : propertiesBuilder.build(),
                flags
        );
    }

    public AvailableNode buildAvailableNode() {
        this.setConcreteConstData();
        return new AvailableNode(new Metadata(label, description, null),
                new Codedata(kind, null, module, null, symbol), true);
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

        public static final String EXPRESSION_LABEL = "Expression";
        public static final String EXPRESSION_KEY = "expression";
        public static final String EXPRESSION_DOC = "Expression";

        public static final String CONDITION_LABEL = "Condition";
        public static final String CONDITION_KEY = "condition";
        public static final String CONDITION_DOC = "Boolean Condition";

        private final Map<String, Property> nodeProperties;
        private final SemanticModel semanticModel;
        protected Property.Builder propertyBuilder;

        public PropertiesBuilder(SemanticModel semanticModel) {
            this.nodeProperties = new LinkedHashMap<>();
            this.propertyBuilder = Property.Builder.getInstance();
            this.semanticModel = semanticModel;
        }

        @SuppressWarnings("unchecked")
        public <T extends PropertiesBuilder> T variable(Node node) {
            if (node == null) {
                return (T) this;
            }
            CommonUtils.getTypeSymbol(semanticModel, node).ifPresent(propertyBuilder::type);
            propertyBuilder
                    .label(VARIABLE_LABEL)
                    .value(CommonUtils.getVariableName(node))
                    .editable()
                    .typeKind(Property.ExpressionTypeKind.BTYPE)
                    .documentation(VARIABLE_DOC);

            addProperty(VARIABLE_KEY, propertyBuilder.build());
            return (T) this;
        }

        public PropertiesBuilder expression(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(propertyBuilder::type);
            Property property = propertyBuilder
                    .label(EXPRESSION_LABEL)
                    .typeKind(Property.ExpressionTypeKind.BTYPE)
                    .documentation(EXPRESSION_DOC)
                    .editable()
                    .value(expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expressionNode).expression().toString() : expressionNode.toString())
                    .build();
            addProperty(EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder callExpression(ExpressionNode expressionNode, ExpressionAttributes.Info info) {
            Property client = Property.Builder.getInstance()
                    .label(info.label())
                    .type(info.type())
                    .value(expressionNode.toString())
                    .typeKind(Property.ExpressionTypeKind.BTYPE)
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
                            .label(info.label())
                            .documentation(info.documentation())
                            .typeKind(Property.ExpressionTypeKind.BTYPE)
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

        public PropertiesBuilder resourceSymbol(ResourceMethodSymbol resourceMethodSymbol) {
            propertyBuilder
                    .label(EVENT_HTTP_API_METHOD)
                    .typeKind(Property.ExpressionTypeKind.IDENTIFIER)
                    .editable()
                    .documentation(EVENT_HTTP_API_METHOD_DOC);
            resourceMethodSymbol.getName().ifPresent(name -> propertyBuilder.value(name));
            addProperty(EVENT_HTTP_API_METHOD_KEY, propertyBuilder.build());

            propertyBuilder
                    .label(EVENT_HTTP_API_PATH)
                    .typeKind(Property.ExpressionTypeKind.URI_PATH)
                    .editable()
                    .documentation(EVENT_HTTP_API_PATH_DOC)
                    .value(resourceMethodSymbol.resourcePath().signature());
            addProperty(EVENT_HTTP_API_PATH_KEY, propertyBuilder.build());
            return this;
        }

        public PropertiesBuilder setConditionExpression(ExpressionNode expressionNode) {
            semanticModel.typeOf(expressionNode).ifPresent(propertyBuilder::type);
            Property condition = propertyBuilder
                    .label(CONDITION_LABEL)
                    .value(expressionNode.toSourceCode())
                    .typeKind(Property.ExpressionTypeKind.BTYPE)
                    .documentation(CONDITION_DOC)
                    .editable()
                    .build();
            addProperty(CONDITION_KEY, condition);
            return this;
        }

        public PropertiesBuilder setExpressionNode(ExpressionNode expressionNode, String expressionDoc) {
            semanticModel.typeOf(expressionNode).ifPresent(propertyBuilder::type);
            Property property = propertyBuilder
                    .label(EXPRESSION_DOC)
                    .value(expressionNode.toSourceCode())
                    .documentation(expressionDoc)
                    .typeKind(Property.ExpressionTypeKind.BTYPE)
                    .editable()
                    .build();
            addProperty(EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder setDefaultExpression(String label, String doc) {
            Property property = propertyBuilder
                    .label(label)
                    .value("")
                    .documentation(doc)
                    .typeKind(Property.ExpressionTypeKind.BTYPE)
                    .editable()
                    .build();
            addProperty(EXPRESSION_KEY, property);
            return this;
        }

        public PropertiesBuilder setDefaultExpression(ExpressionAttributes.Info info) {
            Property property = propertyBuilder
                    .label(info.label())
                    .value("")
                    .type(info.type())
                    .documentation(info.documentation())
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
