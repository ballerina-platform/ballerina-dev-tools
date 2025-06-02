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

import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.Constants;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ExpressionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RemoteActionCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.WaitBuilder;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;

import java.lang.reflect.Type;
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
import java.util.Stack;

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

    private Map<String, Property> nodeProperties;
    private final Stack<Map<String, Property>> nodePropertiesStack;
    private final SemanticModel semanticModel;
    private final DiagnosticHandler diagnosticHandler;
    protected Property.Builder<FormBuilder<T>> propertyBuilder;
    private final ModuleInfo moduleInfo;

    public static final Type NODE_PROPERTIES_TYPE = new TypeToken<Map<String, Property>>() { }.getType();

    public FormBuilder(SemanticModel semanticModel, DiagnosticHandler diagnosticHandler,
                       ModuleInfo moduleInfo, T parentBuilder) {
        super(parentBuilder);
        this.nodeProperties = new LinkedHashMap<>();
        this.propertyBuilder = new Property.Builder<>(this);
        this.nodePropertiesStack = new Stack<>();
        this.semanticModel = semanticModel;
        this.diagnosticHandler = diagnosticHandler;
        this.moduleInfo = moduleInfo;
    }

    public FormBuilder<T> data(Node node, Set<String> names) {
        return data(node, false, names);
    }

    public FormBuilder<T> data(Node node, boolean implicit, Set<String> names) {
        return data(node, implicit ? Property.IMPLICIT_VARIABLE_LABEL : Property.VARIABLE_NAME, Property.VARIABLE_DOC,
                NameUtil.generateTypeName("var", names), false);
    }

    public FormBuilder<T> data(Node node, boolean implicit, Set<String> names, boolean assignment) {
        return data(node, implicit ? Property.IMPLICIT_VARIABLE_LABEL : Property.VARIABLE_NAME, Property.VARIABLE_DOC,
                NameUtil.generateTypeName("var", names), assignment);
    }

    public FormBuilder<T> data(Node node, String label, String doc, String templateName, boolean assignment) {
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(doc)
                    .stepOut()
                .value(node == null ? templateName : CommonUtils.getVariableName(node))
                .type(Property.ValueType.IDENTIFIER);

        if (node != null && !assignment) {
            propertyBuilder.codedata()
                    .lineRange(node.lineRange());
        } else {
            propertyBuilder.editable();
        }

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

    public FormBuilder<T> waitField(Node node) {
        propertyBuilder
                .metadata()
                    .label(Property.VARIABLE_NAME)
                    .description(Property.VARIABLE_DOC)
                    .stepOut()
                .codedata()
                    .dependentProperty(WaitBuilder.WAIT_ALL_KEY)
                    .stepOut()
                .value(node == null ? "" : node.toSourceCode().strip())
                .type(Property.ValueType.IDENTIFIER)
                .editable();
        addProperty(Property.VARIABLE_KEY, node);
        return this;
    }

    public FormBuilder<T> type(Node node, boolean editable) {
        return type(node, Property.TYPE_LABEL, editable);
    }

    public FormBuilder<T> type(Node node, String label, boolean editable) {
        String typeName = (node == null) ? "" : CommonUtils.getVariableName(node);
        return type(typeName, label, editable, node == null ? null : node.lineRange());
    }

    public FormBuilder<T> type(String typeName, boolean editable, String importStatements) {
        return type(typeName, Property.TYPE_LABEL, editable, null, importStatements);
    }

    public FormBuilder<T> type(String typeName, String label, boolean editable, LineRange lineRange) {
        return type(typeName, label, editable, lineRange, null);
    }

    public FormBuilder<T> type(String typeName, String label, boolean editable, LineRange lineRange,
                               String importStatements) {
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(Property.TYPE_DOC)
                    .stepOut()
                .codedata()
                    .stepOut()
                .placeholder("var")
                .value(typeName)
                .imports(importStatements)
                .type(Property.ValueType.TYPE)
                .editable(editable);

        addProperty(Property.TYPE_KEY, lineRange);
        return this;
    }

    public FormBuilder<T> returnType(String value, String typeConstraint, boolean optional) {
        propertyBuilder
                .metadata()
                    .label(Property.RETURN_TYPE_LABEL)
                    .description(Property.RETURN_TYPE_DOC)
                    .stepOut()
                .value(value == null ? "" : value)
                .type(Property.ValueType.TYPE)
                .typeConstraint(typeConstraint)
                .optional(optional)
                .editable();

        addProperty(Property.TYPE_KEY);
        return this;
    }

    public FormBuilder<T> functionDescription(String value) {
        propertyBuilder
                .metadata()
                .label(Property.DESCRIPTION_LABEL)
                .description(Property.DESCRIPTION_TYPE_DOC)
                .stepOut()
                .value(value == null ? "" : value)
                .type(Property.ValueType.STRING)
                .optional(true)
                .editable();

        addProperty(Property.FUNCTION_NAME_DESCRIPTION_KEY);
        return this;
    }

    public FormBuilder<T> returnDescription(String value) {
        propertyBuilder
                .metadata()
                .label(Property.RETURN_DESCRIPTION_LABEL)
                .description(Property.RETURN_DESCRIPTION_TYPE_DOC)
                .stepOut()
                .value(value == null ? "" : value)
                .type(Property.ValueType.STRING)
                .optional(true)
                .editable();

        addProperty(Property.RETURN_DESCRIPTION_KEY);
        return this;
    }

    public FormBuilder<T> returnType(String value) {
        return returnType(value, null, true);
    }

    public FormBuilder<T> dataVariable(TypedBindingPatternNode node, boolean implicit, Set<String> names) {
        return implicit ?
                dataVariable(node, Property.IMPLICIT_VARIABLE_LABEL, Property.IMPLICIT_TYPE_LABEL, true, names)
                : dataVariable(node, Property.VARIABLE_NAME, Property.TYPE_LABEL, true, names);
    }

    public FormBuilder<T> dataVariable(TypedBindingPatternNode node, Set<String> names) {
        return dataVariable(node, false, names);
    }

    public FormBuilder<T> dataVariable(TypedBindingPatternNode node, String variableLabel, String typeDoc,
                                       boolean editable, Set<String> names) {
        data(node == null ? null : node.bindingPattern(), variableLabel, Property.VARIABLE_DOC,
                NameUtil.generateTypeName("var", names), false);

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

    public FormBuilder<T> variableName(String name) {
        return variableName(name, true);
    }

    public FormBuilder<T> variableName(String name, boolean editable) {
        propertyBuilder
                .metadata()
                .label(Property.VARIABLE_NAME)
                .description(Property.VARIABLE_DOC)
                .stepOut()
                .value(name)
                .type(Property.ValueType.IDENTIFIER)
                .typeConstraint(Property.GLOBAL_SCOPE)
                .editable(editable);
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

    public FormBuilder<T> callConnection(ExpressionNode expressionNode, String key) {
        propertyBuilder
                .metadata()
                    .label(Property.CONNECTION_LABEL)
                    .description(Property.CONNECTION_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION)
                .value(expressionNode.toString());
        addProperty(key);
        return this;
    }

    public FormBuilder<T> callExpression(ExpressionNode expressionNode, String key) {
        propertyBuilder
                .metadata()
                    .label(Property.METHOD_EXPRESSION_LABEL)
                    .description(Property.METHOD_EXPRESSION_DOC)
                    .stepOut()
                .type(Property.ValueType.EXPRESSION)
                .value(expressionNode.toString());
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
        return expression(expr, expressionDoc, false, null);
    }

    public FormBuilder<T> expression(String expr, String expressionDoc, boolean optional, String typeConstraint) {
        propertyBuilder
                .metadata()
                    .label(Property.EXPRESSION_DOC)
                    .description(expressionDoc)
                    .stepOut()
                .value(expr)
                .type(Property.ValueType.EXPRESSION)
                .typeConstraint(typeConstraint)
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

    public FormBuilder<T> expression(ExpressionNode expressionNode, String expressionDoc, boolean optional,
                                     String typeConstraint) {
        propertyBuilder
                .metadata()
                    .label(Property.EXPRESSION_DOC)
                    .description(expressionDoc)
                    .stepOut()
                .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                .type(Property.ValueType.EXPRESSION)
                .typeConstraint(typeConstraint)
                .optional(optional)
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

    public FormBuilder<T> expressionOrAction(ExpressionNode expressionNode, String expressionDoc, boolean optional) {
        propertyBuilder
                .metadata()
                    .label(Property.EXPRESSION_DOC)
                    .description(expressionDoc)
                    .stepOut()
                .value(expressionNode == null ? "" : expressionNode.toSourceCode())
                .type(Property.ValueType.ACTION_OR_EXPRESSION)
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

    public FormBuilder<T> defaultValue(ExpressionNode expr) {
        return defaultValue(expr, true);
    }

    public FormBuilder<T> defaultValue(ExpressionNode expr, boolean editable) {
        propertyBuilder
                .metadata()
                .label(Property.DEFAULT_VALUE_LABEL)
                .description(Property.DEFAULT_VALUE_DOC)
                .stepOut()
                .value((expr != null && expr.kind() != SyntaxKind.REQUIRED_EXPRESSION) ? expr.toSourceCode() : null)
                .type(Property.ValueType.EXPRESSION)
                .optional(true)
                .advanced(true)
                .editable(editable);
        addProperty(Property.DEFAULT_VALUE_KEY, expr);
        return this;
    }

    public FormBuilder<T> configValue(ExpressionNode expr) {
        propertyBuilder
                .metadata()
                .label(Property.CONFIG_VALUE_LABEL)
                .description(Property.CONFIG_VALUE_DOC)
                .stepOut()
                .value((expr != null && expr.kind() != SyntaxKind.REQUIRED_EXPRESSION) ? expr.toSourceCode() : null)
                .type(Property.ValueType.EXPRESSION)
                .optional(true)
                .editable();
        addProperty(Property.CONFIG_VALUE_KEY, expr);
        return this;
    }


    public FormBuilder<T> documentation(Node docNode) {
        return documentation(docNode, true);
    }

    public FormBuilder<T> documentation(Node docNode, boolean editable) {
        propertyBuilder
                .metadata()
                .label(Property.CONFIG_VAR_DOC_LABEL)
                .description(Property.CONFIG_VAR_DOC_DOC)
                .stepOut()
                .value(docNode != null ? docNode.toSourceCode() : null)
                .type(Property.ValueType.STRING)
                .optional(true)
                .advanced(true)
                .editable(editable);
        addProperty(Property.CONFIG_VAR_DOC_KEY, docNode);
        return this;
    }

    public FormBuilder<T> statement(Node node) {
        propertyBuilder
                .metadata()
                    .label(ExpressionBuilder.STATEMENT_LABEL)
                    .description(ExpressionBuilder.STATEMENT_DOC)
                    .stepOut()
                .value(node == null ? "" : node.toSourceCode().strip())
                .type(Property.ValueType.STRING)
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

    public FormBuilder<T> functionName(IdentifierToken identifierToken) {
        String functionName = identifierToken.text() == null ? "" : identifierToken.text();

        propertyBuilder
                .metadata()
                    .label(FunctionDefinitionBuilder.FUNCTION_NAME_LABEL)
                    .description(FunctionDefinitionBuilder.FUNCTION_NAME_DOC)
                    .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .typeConstraint(Property.GLOBAL_SCOPE)
                .value(functionName);

        if (!functionName.equals(Constants.MAIN_FUNCTION_NAME)) {
            propertyBuilder.codedata()
                    .lineRange(identifierToken.lineRange());
        }

        addProperty(Property.FUNCTION_NAME_KEY);
        return this;
    }

    public FormBuilder<T> annotations(String annotations) {
        propertyBuilder
                .metadata()
                .label(FunctionDefinitionBuilder.ANNOTATIONS_LABEL)
                .description(FunctionDefinitionBuilder.ANNOTATION_DOC)
                .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .hidden()
                .value(annotations);

        addProperty(Property.ANNOTATIONS_KEY);
        return this;
    }

    public FormBuilder<T> functionNameTemplate(String templatePrefix, Set<String> visibleSymbols) {
        return functionNameTemplate(templatePrefix, visibleSymbols, FunctionDefinitionBuilder.FUNCTION_NAME_LABEL,
                FunctionDefinitionBuilder.FUNCTION_NAME_DOC);
    }

    public FormBuilder<T> functionNameTemplate(String templatePrefix, Set<String> visibleSymbols, String label,
                                               String description) {
        String generatedName = NameUtil.generateTypeName(templatePrefix, visibleSymbols);
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(description)
                    .stepOut()
                .value(generatedName)
                .type(Property.ValueType.IDENTIFIER)
                .typeConstraint(Property.GLOBAL_SCOPE)
                .editable();
        addProperty(Property.FUNCTION_NAME_KEY);
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
                .type(Property.ValueType.ACTION_OR_EXPRESSION);
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

    public FormBuilder<T> qualifiers(List<String> value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                .label(Property.TYPE_QUALIFIERS_LABEL)
                .description(Property.TYPE_QUALIFIERS_DOC)
                .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(value)
                .type(Property.ValueType.MULTIPLE_SELECT);
        addProperty(Property.QUALIFIERS_KEY);
        return this;
    }

    public FormBuilder<T> isPublic(boolean value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.IS_PUBLIC_LABEL)
                    .description(Property.IS_PUBLIC_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(String.valueOf(value))
                .type(Property.ValueType.FLAG)
                .editable();
        addProperty(Property.IS_PUBLIC_KEY);
        return this;
    }

    public FormBuilder<T> isPrivate(boolean value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                .label(Property.IS_PRIVATE_LABEL)
                .description(Property.IS_PRIVATE_DOC)
                .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(String.valueOf(value))
                .type(Property.ValueType.FLAG)
                .editable();
        addProperty(Property.IS_PRIVATE_KEY);
        return this;
    }

    public FormBuilder<T> isIsolated(boolean value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.IS_ISOLATED_LABEL)
                    .description(Property.IS_ISOLATED_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(String.valueOf(value))
                .type(Property.ValueType.FLAG)
                .editable();
        addProperty(Property.IS_ISOLATED_KEY);
        return this;
    }

    public FormBuilder<T> isReadOnly(boolean value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.IS_READ_ONLY_LABEL)
                    .description(Property.IS_READ_ONLY_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(String.valueOf(value))
                .type(Property.ValueType.FLAG)
                .editable();
        addProperty(Property.IS_READ_ONLY_KEY);
        return this;
    }

    public FormBuilder<T> isDistinct(boolean value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.IS_DISTINCT_LABEL)
                    .description(Property.IS_DISTINCT_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(String.valueOf(value))
                .type(Property.ValueType.FLAG)
                .editable();
        addProperty(Property.IS_DISTINCT_KEY);
        return this;
    }

    public FormBuilder<T> networkQualifier(String value, boolean optional, boolean editable, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(Property.NETWORK_QUALIFIER_LABEL)
                    .description(Property.NETWORK_QUALIFIER_DOC)
                    .stepOut()
                .editable(editable)
                .optional(optional)
                .advanced(advanced)
                .value(value)
                .typeConstraint(List.of("service", "client"))
                .type(Property.ValueType.SINGLE_SELECT);
        addProperty(Property.NETWORK_QUALIFIER_KEY);
        return this;
    }

    public FormBuilder<T> waitAll(boolean value) {
        propertyBuilder
                .metadata()
                    .label(WaitBuilder.WAIT_ALL_LABEL)
                    .description(WaitBuilder.WAIT_ALL_DOC)
                    .stepOut()
                .value(value)
                .type(Property.ValueType.FLAG)
                .editable();
        addProperty(WaitBuilder.WAIT_ALL_KEY);
        return this;
    }

    public FormBuilder<T> parameter(String type, String name, Token token, Property.ValueType valueType,
                                    Object typeConstraint) {
        nestedProperty();

        // Build the parameter type property
        propertyBuilder
                .metadata()
                    .label(Property.IMPLICIT_TYPE_LABEL)
                    .description(Property.PARAMETER_TYPE_DOC)
                    .stepOut()
                .type(valueType)
                .typeConstraint(typeConstraint)
                .value(type)
                .editable();
        addProperty(Property.TYPE_KEY);

        // Build the parameter name property
        propertyBuilder
                .metadata()
                    .label(Property.IMPLICIT_VARIABLE_LABEL)
                    .description(Property.PARAMETER_VARIABLE_DOC)
                    .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .editable()
                .value(name);

        if (token == null) {
            propertyBuilder.editable();
        } else {
            propertyBuilder.codedata()
                    .lineRange(token.lineRange());
        }

        addProperty(Property.VARIABLE_KEY);

        return endNestedProperty(Property.ValueType.FIXED_PROPERTY, name, Property.PARAMETER_LABEL,
                Property.PARAMETER_DOC);
    }

    public FormBuilder<T> parameterWithDescription(String type, String name, Token token, Property.ValueType valueType,
                                    Object typeConstraint, String description) {
        nestedProperty();

        // Build the parameter type property
        propertyBuilder
                .metadata()
                .label(Property.IMPLICIT_TYPE_LABEL)
                .description(Property.PARAMETER_TYPE_DOC)
                .stepOut()
                .type(valueType)
                .typeConstraint(typeConstraint)
                .value(type)
                .editable();
        addProperty(Property.TYPE_KEY);

        // Build the parameter name property
        propertyBuilder
                .metadata()
                .label(Property.IMPLICIT_VARIABLE_LABEL)
                .description(Property.PARAMETER_VARIABLE_DOC)
                .stepOut()
                .type(Property.ValueType.IDENTIFIER)
                .editable()
                .value(name);

        if (token == null) {
            propertyBuilder.editable();
        } else {
            propertyBuilder.codedata()
                    .lineRange(token.lineRange());
        }

        addProperty(Property.VARIABLE_KEY);

        propertyBuilder
                .metadata()
                .label(Property.DESCRIPTION_LABEL)
                .description(Property.PARAMETER_DESCRIPTION_TYPE_DOC)
                .stepOut()
                .type(Property.ValueType.STRING)
                .value(description)
                .optional(true)
                .editable();
        addProperty(Property.PARAMETER_DESCRIPTION_KEY);

        return endNestedProperty(Property.ValueType.FIXED_PROPERTY, name, Property.PARAMETER_LABEL,
                Property.PARAMETER_DOC);
    }

    public FormBuilder<T> nestedProperty() {
        Map<String, Property> newProperties = new LinkedHashMap<>();
        nodePropertiesStack.push(nodeProperties);
        nodeProperties = newProperties;
        return this;
    }

    public FormBuilder<T> endNestedProperty(Property.ValueType valueType, String key, String label, String doc,
                                            Object typeConstraint, boolean optional, boolean advanced) {
        propertyBuilder
                .metadata()
                    .label(label)
                    .description(doc)
                    .stepOut()
                .value(nodeProperties)
                .typeConstraint(typeConstraint)
                .type(valueType)
                .optional(optional)
                .advanced(advanced);
        if (!nodePropertiesStack.isEmpty()) {
            nodeProperties = nodePropertiesStack.pop();
        }
        addProperty(key);
        return this;
    }

    public FormBuilder<T> endNestedProperty(Property.ValueType valueType, String key, String label, String doc) {
        return endNestedProperty(valueType, key, label, doc, null, false, false);
    }

    public final void addProperty(String key, Node node) {
        if (node != null && diagnosticHandler != null) {
            diagnosticHandler.handle(propertyBuilder, node.lineRange(), true);
        }
        Property property = propertyBuilder.build();
        this.nodeProperties.put(key, property);
    }

    public final void addProperty(String key) {
        addProperty(key, (Node) null);
    }

    public final void addProperty(String key, LineRange lineRange) {
        if (lineRange != null && diagnosticHandler != null) {
            diagnosticHandler.handle(propertyBuilder, lineRange, true);
        }
        Property property = propertyBuilder.build();
        this.nodeProperties.put(key, property);
    }

    public Map<String, Property> build() {
        return this.nodeProperties;
    }
}
