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
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ExpressionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RemoteActionCallBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.FUNCTION_NAME_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.FUNCTION_NAME_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.FUNCTION_NAME_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.INPUTS_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.INPUTS_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.INPUTS_LABEL;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.OUTPUT_DOC;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.OUTPUT_KEY;
import static io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder.OUTPUT_LABEL;

/**
 * Represents a builder for the form of a flow node.
 *
 * @param <T> Parent builder type
 * @since 2.0.0
 */
public class FormBuilder<T> extends FacetedBuilder<T> {

    private final Map<String, Property> nodeProperties;
    private final SemanticModel semanticModel;
    private final DiagnosticHandler diagnosticHandler;
    protected Property.Builder<FormBuilder<T>> propertyBuilder;
    private final ModuleInfo moduleInfo;

    public FormBuilder(SemanticModel semanticModel, DiagnosticHandler diagnosticHandler,
                       ModuleInfo moduleInfo, T parentBuilder) {
        super(parentBuilder);
        this.nodeProperties = new LinkedHashMap<>();
        this.propertyBuilder = new Property.Builder<>(this);
        this.semanticModel = semanticModel;
        this.diagnosticHandler = diagnosticHandler;
        this.moduleInfo = moduleInfo;
    }

    public FormBuilder<T> data(Node node, Set<String> names) {
        return data(node, false, names);
    }

    public FormBuilder<T> data(Node node, boolean implicit, Set<String> names) {
        return data(node, implicit ? Property.IMPLICIT_VARIABLE_LABEL : Property.VARIABLE_NAME, names);
    }

    public FormBuilder<T> data(Node node, String label, Set<String> names) {
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(Property.VARIABLE_DOC)
                    .stepOut()
                .value(node == null ? NameUtil.generateTypeName("var", names) :
                        CommonUtils.getVariableName(node))
                .type(Property.ValueType.IDENTIFIER)
                .editable();
        addProperty(Property.VARIABLE_KEY, node);

        return this;
    }

    public FormBuilder<T> data(String typeSignature, Set<String> names, String label) {
        String varName = typeSignature.contains(RemoteActionCallBuilder.TARGET_TYPE_KEY)
                ? NameUtil.generateTypeName("var", names)
                : NameUtil.generateVariableName(typeSignature, names);
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(Property.VARIABLE_DOC)
                    .stepOut()
                .value(varName)
                .type(Property.ValueType.IDENTIFIER)
                .editable();
        addProperty(Property.VARIABLE_KEY);
        return this;
    }

    public FormBuilder<T> type(Node node, boolean editable) {
        return type(node, Property.TYPE_LABEL, editable);
    }

    public FormBuilder<T> type(Node node, String label, boolean editable) {
        String typeName = (node == null) ? "" : CommonUtils.getVariableName(node);
        return type(typeName, label, editable, node == null ? null : node.lineRange());
    }

    public FormBuilder<T> type(String typeName, boolean editable) {
        return type(typeName, Property.TYPE_LABEL, editable, null);
    }

    public FormBuilder<T> type(String typeName, String label, boolean editable, LineRange lineRange) {
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(Property.TYPE_DOC)
                    .stepOut()
                .placeholder("var")
                .value(typeName)
                .type(Property.ValueType.TYPE)
                .editable(editable);

        addProperty(Property.TYPE_KEY, lineRange);
        return this;
    }

    public FormBuilder<T> dataVariable(TypedBindingPatternNode node, boolean implicit, Set<String> names) {
        return implicit ?
                dataVariable(node, Property.IMPLICIT_VARIABLE_LABEL, Property.IMPLICIT_TYPE_LABEL, true, names)
                : dataVariable(node, Property.VARIABLE_NAME, Property.TYPE_LABEL, true, names);
    }

    public FormBuilder<T> dataVariable(TypedBindingPatternNode node, Set<String> names) {
        return dataVariable(node, false, names);
    }

    public FormBuilder<T> dataVariable(TypedBindingPatternNode node, String variableDoc, String typeDoc,
                                       boolean editable, Set<String> names) {
        data(node == null ? null : node.bindingPattern(), variableDoc, names);

        String typeName = node == null ? "" : CommonUtils.getTypeSymbol(semanticModel, node)
                .map(typeSymbol -> CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleInfo))
                .orElse(CommonUtils.getVariableName(node));
        return type(typeName, typeDoc, editable, node == null ? null : node.typeDescriptor().lineRange());
    }

    public Property.Builder<FormBuilder<T>> custom() {
        return propertyBuilder;
    }

    public FormBuilder<T> payload(TypedBindingPatternNode node, String type) {
        data(node, new HashSet<>());

        propertyBuilder
                .metadata()
                    .label(Property.TYPE_LABEL)
                    .description(Property.TYPE_DOC)
                    .stepOut()
                .type(Property.ValueType.TYPE)
                .editable();

        if (node == null) {
            propertyBuilder.value(type);
        } else {
            Optional<TypeSymbol> optTypeSymbol = CommonUtils.getTypeSymbol(semanticModel, node);
            optTypeSymbol.ifPresent(typeSymbol -> propertyBuilder.value(
                    CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleInfo)));
        }
        addProperty(Property.TYPE_KEY);
        return this;
    }

    public FormBuilder<T> defaultableName(String data) {
        propertyBuilder
                .metadata()
                    .label(Property.VARIABLE_NAME)
                    .description(Property.VARIABLE_DOC)
                    .stepOut()
                .value(data)
                .type(Property.ValueType.IDENTIFIER)
                .editable();
        addProperty(Property.VARIABLE_KEY);
        return this;
    }

    public FormBuilder<T> patterns(NodeList<? extends Node> node) {
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

    public FormBuilder<T> callExpression(ExpressionNode expressionNode, String key) {
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

    public FormBuilder<T> resourcePath(String path, boolean editable) {
        propertyBuilder
                .metadata()
                    .label(Property.RESOURCE_PATH_LABEL)
                    .description(Property.RESOURCE_PATH_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION);
        if (editable) {
            propertyBuilder
                    .codedata()
                        .originalName(ParamUtils.REST_RESOURCE_PATH)
                        .stepOut()
                    .value(path)
                    .editable();
        } else {
            propertyBuilder
                    .codedata()
                        .originalName(path)
                        .stepOut()
                    .value(path.replaceAll("\\\\", ""));
        }
        addProperty(Property.RESOURCE_PATH_KEY);
        return this;
    }

    public FormBuilder<T> checkError(boolean checkError) {
        return checkError(checkError, Property.CHECK_ERROR_DOC, true);
    }

    public FormBuilder<T> checkError(boolean checkError, String doc, boolean editable) {
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
    public FormBuilder<T> inputs(SeparatedNodeList<FunctionArgumentNode> arguments,
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
                    moduleInfo);
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

    public FormBuilder<T> output(Node node) {
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
                        CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleInfo)));

        addProperty(OUTPUT_KEY, node);
        return this;
    }

    public FormBuilder<T> functionArguments(SeparatedNodeList<FunctionArgumentNode> arguments,
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

    public FormBuilder<T> condition(ExpressionNode expressionNode) {
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

    public FormBuilder<T> retryCount(int retryCount) {
        return retryCount(retryCount, false);
    }

    public FormBuilder<T> retryCount(int retryCount, boolean optional) {
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

    public FormBuilder<T> expression(String expr, String expressionDoc) {
        return expression(expr, expressionDoc, false);
    }

    public FormBuilder<T> expression(String expr, String expressionDoc, boolean optional) {
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

    public FormBuilder<T> expression(ExpressionNode expressionNode, String expressionDoc) {
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

    public FormBuilder<T> expression(ExpressionNode expressionNode, String key, String expressionDoc) {
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

    public FormBuilder<T> expression(ExpressionNode expressionNode, String expressionDoc, boolean optional) {
        propertyBuilder
                .metadata()
                    .label(Property.EXPRESSION_DOC)
                    .description(expressionDoc)
                    .stepOut()
                .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                .type(Property.ValueType.EXPRESSION)
                .optional(optional)
                .editable();
        addProperty(Property.EXPRESSION_KEY, expressionNode);
        return this;
    }

    public FormBuilder<T> expression(ExpressionNode expressionNode) {
        return expression(expressionNode, false);
    }

    public FormBuilder<T> expression(ExpressionNode expressionNode, boolean optional) {
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

    public FormBuilder<T> defaultableVariable(ExpressionNode expr) {
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

    public FormBuilder<T> statement(Node node) {
        propertyBuilder
                .metadata()
                    .label(ExpressionBuilder.STATEMENT_LABEL)
                    .description(ExpressionBuilder.STATEMENT_DOC)
                    .stepOut()
                .value(node == null ? "" : node.toSourceCode().strip())
                .type(Property.ValueType.EXPRESSION)
                .editable();
        addProperty(ExpressionBuilder.STATEMENT_KEY, node);
        return this;
    }

    public FormBuilder<T> ignore(boolean ignore) {
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

    public FormBuilder<T> comment(String comment) {
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

    public FormBuilder<T> onErrorVariable(TypedBindingPatternNode typedBindingPatternNode) {
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
                            CommonUtils.getTypeSignature(semanticModel, typeSymbol, false, moduleInfo)));
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

    public FormBuilder<T> functionName(String functionName) {
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

    public FormBuilder<T> scope(String scope) {
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

    public FormBuilder<T> view(LineRange lineRange) {
        propertyBuilder
                .metadata()
                    .label(DataMapperBuilder.VIEW_LABEL)
                    .description(DataMapperBuilder.VIEW_DOC)
                    .stepOut()
                .value(lineRange)
                .type(Property.ValueType.VIEW);
        addProperty(DataMapperBuilder.VIEW_KEY);
        return this;
    }

    public FormBuilder<T> collection(Node expressionNode) {
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

    public FormBuilder<T> name(String value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.TYPE_NAME_LABEL)
                    .description(Property.TYPE_NAME_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(value)
                .type(Property.ValueType.IDENTIFIER);
        addProperty(Property.NAME_KEY);
        return this;
    }

    public FormBuilder<T> description(String value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.TYPE_DESC_LABEL)
                    .description(Property.TYPE_DESC_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(value)
                .type(Property.ValueType.STRING);
        addProperty(Property.DESCRIPTION_KEY);
        return this;
    }

    public FormBuilder<T> isArray(String value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.TYPE_IS_ARRAY_LABEL)
                    .description(Property.TYPE_IS_ARRAY_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(value)
                .type(Property.ValueType.FLAG);
        addProperty(Property.IS_ARRAY_KEY);
        return this;
    }

    public FormBuilder<T> arraySize(String value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.TYPE_ARRAY_SIZE_LABEL)
                    .description(Property.TYPE_ARRAY_SIZE_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(value)
                .type(Property.ValueType.STRING);
        addProperty(Property.ARRAY_SIZE);
        return this;
    }

    public final void addProperty(String key) {
        addProperty(key, (Node) null);
    }

    public final void addProperty(String key, LineRange lineRange) {
        if (lineRange != null) {
            diagnosticHandler.handle(propertyBuilder, lineRange, true);
        }
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
