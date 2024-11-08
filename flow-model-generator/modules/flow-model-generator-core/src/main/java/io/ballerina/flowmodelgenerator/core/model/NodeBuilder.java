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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
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
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Assign;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryData;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Comment;
import io.ballerina.flowmodelgenerator.core.model.node.Commit;
import io.ballerina.flowmodelgenerator.core.model.node.ConfigVariable;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapper;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.EventStart;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.Foreach;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionCall;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayload;
import io.ballerina.flowmodelgenerator.core.model.node.Lock;
import io.ballerina.flowmodelgenerator.core.model.node.Match;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnection;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.ResourceActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Retry;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Rollback;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.Stop;
import io.ballerina.flowmodelgenerator.core.model.node.Transaction;
import io.ballerina.flowmodelgenerator.core.model.node.Variable;
import io.ballerina.flowmodelgenerator.core.model.node.While;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayload;
import io.ballerina.projects.Document;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.FUNCTION_NAME_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.FUNCTION_NAME_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.FUNCTION_NAME_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.INPUTS_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.INPUTS_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.INPUTS_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.OUTPUT_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.OUTPUT_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapper.OUTPUT_LABEL;

/**
 * Represents a builder for the flow node.
 *
 * @since 1.4.0
 */
public abstract class NodeBuilder implements DiagnosticHandler.DiagnosticCapable {

    protected List<Branch> branches;
    protected Metadata.Builder<NodeBuilder> metadataBuilder;
    protected Codedata.Builder<NodeBuilder> codedataBuilder;
    protected PropertiesBuilder<NodeBuilder> propertiesBuilder;
    protected Diagnostics.Builder<NodeBuilder> diagnosticBuilder;
    protected DiagnosticHandler diagnosticHandler;
    protected int flags;
    protected boolean returning;
    protected SemanticModel semanticModel;
    protected FlowNode cachedFlowNode;
    protected ModuleDescriptor moduleDescriptor;

    private static final Map<NodeKind, Supplier<? extends NodeBuilder>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(NodeKind.IF, If::new);
        put(NodeKind.RETURN, Return::new);
        put(NodeKind.EXPRESSION, DefaultExpression::new);
        put(NodeKind.ERROR_HANDLER, ErrorHandler::new);
        put(NodeKind.WHILE, While::new);
        put(NodeKind.CONTINUE, Continue::new);
        put(NodeKind.BREAK, Break::new);
        put(NodeKind.PANIC, Panic::new);
        put(NodeKind.EVENT_START, EventStart::new);
        put(NodeKind.REMOTE_ACTION_CALL, ActionCall::new);
        put(NodeKind.RESOURCE_ACTION_CALL, ResourceActionCall::new);
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
        put(NodeKind.VARIABLE, Variable::new);
        put(NodeKind.ASSIGN, Assign::new);
        put(NodeKind.COMMENT, Comment::new);
        put(NodeKind.MATCH, Match::new);
        put(NodeKind.CONFIG_VARIABLE, ConfigVariable::new);
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

    public NodeBuilder diagnosticHandler(DiagnosticHandler diagnosticHandler) {
        this.diagnosticHandler = diagnosticHandler;
        return this;
    }

    public NodeBuilder defaultModuleName(ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
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

    public NodeBuilder symbolInfo(Symbol symbol) {
        Optional<ModuleSymbol> module = symbol.getModule();
        if (module.isEmpty()) {
            codedata()
                    .module(moduleDescriptor.name().packageName().value())
                    .version("0.0.0");
            return this;
        }

        ModuleID moduleId = module.get().id();
        String orgName = moduleId.orgName();
        String packageName = moduleId.packageName();
        String versionName = moduleId.version();

        if (!CommonUtils.isDefaultPackage(orgName, packageName, moduleDescriptor)) {
            metadata().icon(CommonUtils.generateIcon(orgName, packageName, versionName));
        }
        codedata()
                .org(orgName)
                .module(packageName)
                .version(versionName);
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
            this.propertiesBuilder = new PropertiesBuilder<>(semanticModel, diagnosticHandler, moduleDescriptor, this);
        }
        return this.propertiesBuilder;
    }

    public Diagnostics.Builder<NodeBuilder> diagnostics() {
        if (this.diagnosticBuilder == null) {
            this.diagnosticBuilder = new Diagnostics.Builder<>(this);
        }
        return this.diagnosticBuilder;
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
                diagnosticBuilder == null ? null : diagnosticBuilder.build(),
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

        public Set<String> getAllVisibleSymbolNames() {
            try {
                workspaceManager.loadProject(filePath);
                SemanticModel semanticModel =
                        workspaceManager.semanticModel(filePath).orElseThrow();
                Document document = workspaceManager.document(filePath).orElseThrow();
                return semanticModel.visibleSymbols(document, position).parallelStream()
                        .filter(s -> s.getName().isPresent())
                        .map(s -> s.getName().get())
                        .collect(Collectors.toSet());
            } catch (Throwable e) {
                return new HashSet<>();
            }
        }
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
        private final DiagnosticHandler diagnosticHandler;
        protected Property.Builder<PropertiesBuilder<T>> propertyBuilder;
        private final ModuleDescriptor moduleDescriptor;

        public PropertiesBuilder(SemanticModel semanticModel, DiagnosticHandler diagnosticHandler,
                                 ModuleDescriptor moduleDescriptor, T parentBuilder) {
            super(parentBuilder);
            this.nodeProperties = new LinkedHashMap<>();
            this.propertyBuilder = new Property.Builder<>(this);
            this.semanticModel = semanticModel;
            this.diagnosticHandler = diagnosticHandler;
            this.moduleDescriptor = moduleDescriptor;
        }

        public PropertiesBuilder<T> variable(Node node, boolean implicit) {
            propertyBuilder
                    .metadata()
                        .label(implicit ? Property.DATA_IMPLICIT_VARIABLE_LABEL : Property.VARIABLE_LABEL)
                        .description(Property.VARIABLE_DOC)
                        .stepOut()
                    .value(node == null ? "" : CommonUtils.getVariableName(node))
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();

            addProperty(Property.VARIABLE_KEY, node);
            return this;
        }

        public PropertiesBuilder<T> variable(Node node) {
            return variable(node, false);
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

            addProperty(Property.DATA_TYPE_KEY);
            return this;
        }

        public PropertiesBuilder<T> type(String typeName, boolean editable) {
            propertyBuilder
                    .metadata()
                        .label(Property.DATA_TYPE_LABEL)
                        .description(Property.DATA_TYPE_DOC)
                        .stepOut()
                    .value(typeName)
                    .type(Property.ValueType.TYPE);
            if (editable) {
                propertyBuilder.editable();
            }

            addProperty(Property.DATA_TYPE_KEY);
            return this;
        }

        public PropertiesBuilder<T> dataVariable(TypedBindingPatternNode node, boolean implicit, Set<String> names) {
            return implicit ?
                    dataVariable(node, Property.DATA_IMPLICIT_VARIABLE_LABEL, Property.DATA_IMPLICIT_TYPE_LABEL, names)
                    : dataVariable(node, Property.DATA_VARIABLE_LABEL, Property.DATA_TYPE_LABEL, names);
        }

        public PropertiesBuilder<T> dataVariable(TypedBindingPatternNode node, Set<String> names) {
            return dataVariable(node, false, names);
        }

        public PropertiesBuilder<T> dataVariable(TypedBindingPatternNode node, String variableDoc, String typeDoc,
                                                 Set<String> names) {
            data(node, variableDoc, names);

            propertyBuilder
                    .metadata()
                        .label(typeDoc)
                        .description(Property.DATA_TYPE_DOC)
                        .stepOut()
                    .placeholder("var")
                    .type(Property.ValueType.TYPE)
                    .editable();

            if (node == null) {
                propertyBuilder.value("");
            } else {
                Optional<TypeSymbol> optTypeSymbol = CommonUtils.getTypeSymbol(semanticModel, node);
                optTypeSymbol.ifPresent(typeSymbol -> propertyBuilder.value(
                        CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleDescriptor)));
            }

            addProperty(Property.DATA_TYPE_KEY, node == null ? null : node.typeDescriptor());
            return this;
        }

        public PropertiesBuilder<T> payload(TypedBindingPatternNode node, String type) {
            data(node, new HashSet<>());

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
                        CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleDescriptor)));
            }
            addProperty(Property.DATA_TYPE_KEY);
            return this;
        }

        public PropertiesBuilder<T> data(TypedBindingPatternNode node, Set<String> names) {
            return data(node, false, names);
        }

        public PropertiesBuilder<T> data(TypedBindingPatternNode node, boolean implicit, Set<String> names) {
            return data(node, implicit ? Property.DATA_IMPLICIT_VARIABLE_LABEL : Property.DATA_VARIABLE_LABEL, names);
        }

        public PropertiesBuilder<T> data(TypedBindingPatternNode node, String label, Set<String> names) {
            propertyBuilder
                    .metadata()
                        .label(label)
                        .description(Property.DATA_VARIABLE_DOC)
                        .stepOut()
                    .value(node == null ? NameUtil.generateTypeName("var", names) :
                            CommonUtils.getVariableName(node))
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();
            addProperty(Property.DATA_VARIABLE_KEY, node == null ? null : node.bindingPattern());

            return this;
        }

        public PropertiesBuilder<T> data(String typeSignature, Set<String> names, String label) {
            String varName = typeSignature.contains(ActionCall.TARGET_TYPE_KEY)
                    ? NameUtil.generateTypeName("var", names)
                    : NameUtil.generateVariableName(typeSignature, names);
            propertyBuilder
                    .metadata()
                        .label(label)
                        .description(Property.DATA_VARIABLE_DOC)
                        .stepOut()
                    .value(varName)
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();
            addProperty(Property.DATA_VARIABLE_KEY);
            return this;
        }

        public PropertiesBuilder<T> defaultableName(String data) {
            propertyBuilder
                    .metadata()
                        .label(Property.DATA_VARIABLE_LABEL)
                        .description(Property.DATA_VARIABLE_DOC)
                        .stepOut()
                    .value(data)
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();
            addProperty(Property.DATA_VARIABLE_KEY);
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

            propertyBuilder
                    .metadata()
                        .label(Property.PATTERNS_LABEL)
                        .description(Property.PATTERNS_DOC)
                        .stepOut()
                    .value(properties)
                    .type(Property.ValueType.SINGLE_SELECT)
                    .editable();
            addProperty(Property.PATTERNS_KEY);

            return this;
        }

        public PropertiesBuilder<T> callExpression(ExpressionNode expressionNode, String key) {
            propertyBuilder
                    .metadata()
                        .label(Property.CONNECTION_LABEL)
                        .description(Property.CONNECTION_DOC)
                        .stepOut()
                    .type(Property.ValueType.EXPRESSION)
                    .value(expressionNode.toString())
                    .type(Property.ValueType.EXPRESSION);
            addProperty(key);
            return this;
        }

        public PropertiesBuilder<T> resourcePath(String path) {
            propertyBuilder
                    .metadata()
                        .label(Property.RESOURCE_PATH_LABEL)
                        .description(Property.RESOURCE_PATH_DOC)
                        .stepOut()
                    .type(Property.ValueType.EXPRESSION)
                    .value(path)
                    .editable();
            addProperty(Property.RESOURCE_PATH_KEY);
            return this;
        }

        public PropertiesBuilder<T> checkError(boolean checkError) {
            return checkError(checkError, Property.CHECK_ERROR_DOC, true);
        }

        public PropertiesBuilder<T> checkError(boolean checkError, String doc, boolean editable) {
            propertyBuilder
                    .metadata()
                        .label(Property.CHECK_ERROR_LABEL)
                        .description(doc)
                        .stepOut()
                    .value(checkError)
                    .advanced(true)
                    .type(Property.ValueType.FLAG);
            if (editable) {
                propertyBuilder.editable();
            }
            addProperty(Property.CHECK_ERROR_KEY);
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
                        moduleDescriptor);
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

            addProperty(INPUTS_KEY);
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
                            CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleDescriptor)));

            addProperty(OUTPUT_KEY, node);
            return this;
        }

        public PropertiesBuilder<T> functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
                                                      List<ParameterSymbol> parameterSymbols,
                                                      Map<String, String> documentationMap,
                                                      boolean ignoreTargetType) {
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

            int numParams = parameterSymbols.size();
            int numPositionalArgs = positionalArgs.size();

            for (int i = 0; i < numParams; i++) {
                ParameterSymbol parameterSymbol = parameterSymbols.get(i);

                if (ignoreTargetType && parameterSymbol.nameEquals("targetType")) {
                    continue;
                }

                Optional<String> name = parameterSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }

                String parameterName = name.get().startsWith("'") ? name.get().substring(1) : name.get();
                Node paramValue = i < numPositionalArgs ? positionalArgs.poll() : namedArgValueMap.get(parameterName);

                propertyBuilder
                        .metadata()
                            .label(parameterName)
                            .description(documentationMap.get(parameterName))
                            .stepOut()
                        .type(Property.ValueType.EXPRESSION)
                        .editable()
                        .defaultable(parameterSymbol.paramKind() == ParameterKind.DEFAULTABLE);

                if (paramValue != null) {
                    propertyBuilder.value(paramValue.toSourceCode());
                }

                addProperty(parameterName, paramValue);
            }
            return this;
        }

        public PropertiesBuilder<T> condition(ExpressionNode expressionNode) {
            propertyBuilder
                    .metadata()
                        .label(Property.CONDITION_LABEL)
                        .description(Property.CONDITION_DOC)
                        .stepOut()
                    .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                    .placeholder("true")
                    .type(Property.ValueType.EXPRESSION)
                    .editable();
            addProperty(Property.CONDITION_KEY, expressionNode);
            return this;
        }

        public PropertiesBuilder<T> retryCount(int retryCount) {
            return retryCount(retryCount, false);
        }

        public PropertiesBuilder<T> retryCount(int retryCount, boolean optional) {
            propertyBuilder
                    .metadata()
                        .label(Property.RETRY_COUNT_LABEL)
                        .description(Property.RETRY_COUNT_DOC)
                        .stepOut()
                    .value(String.valueOf(retryCount))
                    .type(Property.ValueType.EXPRESSION)
                    .optional(optional)
                    .editable();
            addProperty(Property.RETRY_COUNT_KEY);
            return this;
        }

        public PropertiesBuilder<T> expression(String expr, String expressionDoc) {
            return expression(expr, expressionDoc, false);
        }

        public PropertiesBuilder<T> expression(String expr, String expressionDoc, boolean optional) {
            propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_DOC)
                        .description(expressionDoc)
                        .stepOut()
                    .value(expr)
                    .type(Property.ValueType.EXPRESSION)
                    .optional(optional)
                    .editable();
            addProperty(Property.EXPRESSION_KEY);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode, String expressionDoc) {
            propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_DOC)
                        .description(expressionDoc)
                        .stepOut()
                    .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                    .type(Property.ValueType.EXPRESSION)
                    .editable();
            addProperty(Property.EXPRESSION_KEY, expressionNode);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode, String key, String expressionDoc) {
            propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_DOC)
                        .description(expressionDoc)
                        .stepOut()
                    .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                    .type(Property.ValueType.EXPRESSION)
                    .editable();
            addProperty(key, expressionNode);
            return this;
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode) {
            return expression(expressionNode, false);
        }

        public PropertiesBuilder<T> expression(ExpressionNode expressionNode, boolean optional) {
            propertyBuilder
                    .metadata()
                        .label(Property.EXPRESSION_LABEL)
                        .description(Property.EXPRESSION_DOC)
                        .stepOut()
                    .editable()
                    .value(expressionNode == null ? "" : expressionNode.toString())
                    .optional(optional)
                    .type(Property.ValueType.EXPRESSION);
            addProperty(Property.EXPRESSION_KEY, expressionNode);
            return this;
        }

        public PropertiesBuilder<T> defaultableVariable(ExpressionNode expr) {
            propertyBuilder
                    .metadata()
                        .label(Property.DEFAULT_VALUE_LABEL)
                        .description(Property.DEFAULT_VALUE_DOC)
                        .stepOut()
                    .value((expr != null && expr.kind() != SyntaxKind.REQUIRED_EXPRESSION) ? expr.toSourceCode() : "")
                    .type(Property.ValueType.EXPRESSION)
                    .editable();
            addProperty(Property.DEFAULTABLE_KEY, expr);
            return this;
        }

        public PropertiesBuilder<T> statement(Node node) {
            propertyBuilder
                    .metadata()
                        .label(DefaultExpression.STATEMENT_LABEL)
                        .description(DefaultExpression.STATEMENT_DOC)
                        .stepOut()
                    .value(node == null ? "" : node.toSourceCode().strip())
                    .type(Property.ValueType.EXPRESSION)
                    .editable();
            addProperty(DefaultExpression.STATEMENT_KEY, node);
            return this;
        }

        public PropertiesBuilder<T> ignore(boolean ignore) {
            propertyBuilder
                    .metadata()
                        .label(Property.IGNORE_LABEL)
                        .description(Property.IGNORE_DOC)
                        .stepOut()
                    .value(String.valueOf(ignore))
                    .type(Property.ValueType.EXPRESSION)
                    .editable();
            addProperty(Property.IGNORE_KEY);
            return this;
        }

        public PropertiesBuilder<T> comment(String comment) {
            propertyBuilder
                    .metadata()
                        .label(Property.COMMENT_LABEL)
                        .description(Property.COMMENT_DOC)
                        .stepOut()
                    .value(comment)
                    .type(Property.ValueType.STRING)
                    .editable();
            addProperty(Property.COMMENT_KEY);
            return this;
        }

        public PropertiesBuilder<T> onErrorVariable(TypedBindingPatternNode typedBindingPatternNode) {
            propertyBuilder
                    .metadata()
                        .label(Property.ON_ERROR_VARIABLE_LABEL)
                        .description(Property.ON_ERROR_VARIABLE_DOC)
                        .stepOut()
                    .value(typedBindingPatternNode == null ? "" :
                            typedBindingPatternNode.bindingPattern().toString())
                    .placeholder("err")
                    .type(Property.ValueType.IDENTIFIER)
                    .editable();
            addProperty(Property.ON_ERROR_VARIABLE_KEY,
                    typedBindingPatternNode == null ? null : typedBindingPatternNode.bindingPattern());

            if (typedBindingPatternNode == null) {
                propertyBuilder.value("");
            } else {
                CommonUtils.getTypeSymbol(semanticModel, typedBindingPatternNode)
                        .ifPresent(typeSymbol -> propertyBuilder.value(
                                CommonUtils.getTypeSignature(semanticModel, typeSymbol, false, moduleDescriptor)));
            }
            propertyBuilder
                    .metadata()
                        .label(Property.ON_ERROR_TYPE_LABEL)
                        .description(Property.ON_ERROR_TYPE_DOC)
                        .stepOut()
                    .placeholder("error")
                    .editable()
                    .type(Property.ValueType.TYPE);
            addProperty(Property.ON_ERROR_TYPE_KEY);

            return this;
        }

        public PropertiesBuilder<T> functionName(String functionName) {
            propertyBuilder
                    .metadata()
                        .label(FUNCTION_NAME_LABEL)
                        .description(FUNCTION_NAME_DOC)
                        .stepOut()
                    .type(Property.ValueType.IDENTIFIER)
                    .value(functionName)
                    .editable();

            addProperty(FUNCTION_NAME_KEY);
            return this;
        }

        public Property.Builder<PropertiesBuilder<T>> custom() {
            return propertyBuilder;
        }

        public PropertiesBuilder<T> scope(String scope) {
            propertyBuilder
                    .metadata()
                        .label(Property.SCOPE_LABEL)
                        .description(Property.SCOPE_DOC)
                        .stepOut()
                    .type(Property.ValueType.ENUM)
                    .value(scope)
                    .advanced(true)
                    .editable();
            addProperty(Property.SCOPE_KEY);
            return this;
        }

        public PropertiesBuilder<T> view(LineRange lineRange) {
            propertyBuilder
                    .metadata()
                        .label(DataMapper.VIEW_LABEL)
                        .description(DataMapper.VIEW_DOC)
                        .stepOut()
                    .value(lineRange)
                    .type(Property.ValueType.VIEW);
            addProperty(DataMapper.VIEW_KEY);
            return this;
        }

        public PropertiesBuilder<T> collection(Node expressionNode) {
            propertyBuilder
                    .metadata()
                        .label(Property.COLLECTION_LABEL)
                        .description(Property.COLLECTION_DOC)
                        .stepOut()
                    .editable()
                    .placeholder("[]")
                    .value(expressionNode == null ? "" : expressionNode.kind() == SyntaxKind.CHECK_EXPRESSION ?
                            ((CheckExpressionNode) expressionNode).expression().toString() : expressionNode.toString())
                    .type(Property.ValueType.EXPRESSION);
            addProperty(Property.COLLECTION_KEY, expressionNode);
            return this;
        }

        public final void addProperty(String key) {
            addProperty(key, (Node) null);
        }

        public final void addProperty(String key, LineRange lineRange) {
            diagnosticHandler.handle(propertyBuilder, lineRange, true);
            Property property = propertyBuilder.build();
            this.nodeProperties.put(key, property);
        }

        public final void addProperty(String key, Node node) {
            if (node != null) {
                diagnosticHandler.handle(propertyBuilder, node.lineRange(), true);
            }
            Property property = propertyBuilder.build();
            this.nodeProperties.put(key, property);
        }

        public Map<String, Property> build() {
            return this.nodeProperties;
        }
    }
}
