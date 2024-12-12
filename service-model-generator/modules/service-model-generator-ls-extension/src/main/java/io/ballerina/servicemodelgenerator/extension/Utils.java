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

package io.ballerina.servicemodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.TriggerProperty;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.TriggerListRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerRequest;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Common utility functions used in the project.
 *
 * @since 2.0.0
 */
public final class Utils {

    private Utils() {
    }

    /**
     * Convert the syntax-node line range into a lsp4j range.
     *
     * @param lineRange line range
     * @return {@link Range} converted range
     */
    public static Range toRange(LineRange lineRange) {
        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param position line position
     * @return {@link Range} converted range
     */
    public static Range toRange(LinePosition position) {
        return new Range(toPosition(position), toPosition(position));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param linePosition - line position
     * @return {@link Position} converted position
     */
    public static Position toPosition(LinePosition linePosition) {
        return new Position(linePosition.line(), linePosition.offset());
    }

    public static void populateProperties(Service service) {
        Value value = service.getProperty("requiredFunctions");
        if (Objects.nonNull(value) && value.isEnabled()) {
            String requiredFunction = value.getValue();
            service.getFunctions()
                    .forEach(function -> function.setEnabled(
                            function.getName().getValue().equals(requiredFunction)));
        }
        Value serviceValue = service.getServiceType();
        if (Objects.nonNull(serviceValue) && serviceValue.isEnabled()) {
            String serviceType = service.getServiceTypeName();
            if (Objects.nonNull(serviceType)) {
                getServiceByServiceType(serviceType.toLowerCase(Locale.ROOT))
                        .ifPresent(serviceTypeModel -> service.setFunctions(serviceTypeModel.getFunctions()));
            }
        }
    }

    private static Optional<Service> getServiceByServiceType(String serviceType) {
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream(String.format("services/%s.json", serviceType.replaceAll(":", ".")));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Service.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Optional<ExpressionNode> getListenerExpression(ServiceDeclarationNode serviceNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        if (expressions.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode expressionNode = expressions.get(0);
        return Optional.of(expressionNode);
    }

    public static Service getServiceModel(ServiceDeclarationNode serviceDeclarationNode) {
        Service serviceModel = Service.getNewService();
        String basePath = getPath(serviceDeclarationNode.absoluteResourcePath());
        if (!basePath.isEmpty()) {
            Value basePathValue = new Value();
            basePathValue.setValue(basePath);
            basePathValue.setValueType("IDENTIFIER");
            basePathValue.setEnabled(true);
            serviceModel.setBasePath(basePathValue);
        }
        Optional<TypeDescriptorNode> serviceTypeDesc = serviceDeclarationNode.typeDescriptor();
        if (serviceTypeDesc.isPresent()) {
            Value serviceType = new Value();
            serviceType.setValue(serviceTypeDesc.get().toString().trim());
            serviceType.setValueType("TYPE");
            serviceType.setEnabled(true);
            serviceModel.setServiceType(serviceType);
        }
        List<Function> functionModels = new ArrayList<>();
        serviceDeclarationNode.members().forEach(member -> {
            if (member instanceof FunctionDefinitionNode functionDefinitionNode) {
                Function functionModel = getFunctionModel(functionDefinitionNode);
                functionModels.add(functionModel);
            }
        });
        serviceModel.setFunctions(functionModels);
        return serviceModel;
    }

    public static String getPath(NodeList<Node> paths) {
        return paths.stream().map(Node::toString).collect(Collectors.joining(""));
    }

    public static Function getFunctionModel(FunctionDefinitionNode functionDefinitionNode) {
        Function functionModel = Function.getNewFunction();
        functionModel.setEnabled(true);
        Value accessor = functionModel.getAccessor();
        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.functionName().text().trim());
        functionName.setValueType("IDENTIFIER");
        functionName.setEnabled(true);
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            if (qualifier.text().trim().matches("remote")) {
                functionModel.setKind("REMOTE");
            } else if (qualifier.text().trim().matches("resource")) {
                functionModel.setKind("RESOURCE");
                accessor.setValue(functionDefinitionNode.functionName().text().trim());
                accessor.setValueType("IDENTIFIER");
                accessor.setEnabled(true);
                functionName.setValue(getPath(functionDefinitionNode.relativeResourcePath()));
            } else {
                functionModel.addQualifier(qualifier.text().trim());
            }
        }
        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            Value returnType = functionModel.getReturnType();
            if (Objects.nonNull(returnType)) {
                returnType.setValue(returnTypeDesc.get().type().toString().trim());
                returnType.setValueType("TYPE");
                returnType.setEnabled(true);
            }
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = getParameterModel(parameterNode);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        return functionModel;
    }

    public static Optional<Parameter> getParameterModel(ParameterNode parameterNode) {
        if (parameterNode instanceof RequiredParameterNode parameter) {
            Parameter parameterModel = Parameter.getNewParameter();
            parameterModel.setKind("REQUIRED");
            Value type = parameterModel.getType();
            type.setValue(parameter.typeName().toString().trim());
            type.setValueType("TYPE");
            type.setType(true);
            type.setEnabled(true);
            Value name = parameterModel.getName();
            name.setValue(parameter.paramName().get().toString().trim());
            name.setValueType("STRING");
            name.setEnabled(true);
            parameterModel.setEnabled(true);
            return Optional.of(parameterModel);
        }
        return Optional.empty();
    }

    public static void updateListenerModel(Listener listener, ListenerDeclarationNode listenerNode) {
        Optional<Listener> commonListener = getListenerModel(listenerNode);
        commonListener.ifPresent(listenerModel -> {
            listenerModel.getProperties().forEach((key, value) -> {
                updateValue(listener.getProperty(key), value);
            });
        });
    }

    public static void updateServiceModel(Service serviceModel, ServiceDeclarationNode serviceNode) {
        if (serviceModel.getModuleName().equals("http")) {
            serviceModel.setFunctions(new ArrayList<>());
        }
        Service commonSvcModel = getServiceModel(serviceNode);
        if (Objects.nonNull(serviceModel.getServiceType()) && Objects.nonNull(commonSvcModel.getServiceType())) {
            serviceModel.updateServiceType(commonSvcModel.getServiceType());
        }
        populateProperties(serviceModel);
        serviceModel.setCodedata(new Codedata(serviceNode.lineRange()));
        updateValue(serviceModel.getBasePath(), commonSvcModel.getBasePath());
        serviceModel.getFunctions().forEach(functionModel -> {
            Optional<Function> function = commonSvcModel.getFunctions().stream()
                    .filter(func -> func.getName().getValue().equals(functionModel.getName().getValue())
                            && func.getKind().equals(functionModel.getKind()))
                    .findFirst();
            function.ifPresentOrElse(
                    func -> updateFunction(functionModel, func, serviceModel),
                    () -> functionModel.setEnabled(false)
            );
        });
        commonSvcModel.getFunctions().forEach(funtionModel -> {
            if (serviceModel.getFunctions().stream()
                    .noneMatch(func -> func.getName().getValue().equals(funtionModel.getName().getValue()))) {
                serviceModel.addFunction(funtionModel);
            }
        });
        Optional<ExpressionNode> listenerExpression = getListenerExpression(serviceNode);
        if (listenerExpression.isPresent() && listenerExpression.get() instanceof SimpleNameReferenceNode listener) {
            serviceModel.getListener().setValue(listener.name().text().trim());
        }
        NodeList<Node> paths = serviceNode.absoluteResourcePath();
        if (!paths.isEmpty()) {
            serviceModel.getBasePath().setValue(getPath(paths));
        }
    }

    public static void updateValue(Value target, Value source) {
        if (Objects.isNull(target) || Objects.isNull(source)) {
            return;
        }
        target.setEnabled(source.isEnabled());
        target.setValue(source.getValue());
        target.setValueType(source.getValueType());
    }

    public static void updateFunction(Function target, Function source, Service service) {
        target.setEnabled(source.isEnabled());
        target.setCodedata(source.getCodedata());
        updateValue(target.getAccessor(), source.getAccessor());
        updateValue(target.getName(), source.getName());
        target.getParameters().forEach(param -> {
            Optional<Parameter> parameter = source.getParameters().stream()
                    .filter(p -> p.getName().getValue().equals(param.getName().getValue()))
                    .findFirst();
            parameter.ifPresent(value -> updateParameter(param, value));
        });
        updateValue(target.getReturnType(), source.getReturnType());
        Value requiredFunctions = service.getProperty("requiredFunctions");
        if (Objects.nonNull(requiredFunctions)) {
            if (source.isEnabled() && requiredFunctions.getItems().contains(source.getName().getValue())) {
                requiredFunctions.setValue(source.getName().getValue());
            }
        }
    }

    public static void updateParameter(Parameter target, Parameter source) {
        target.setEnabled(source.isEnabled());
        target.setKind(source.getKind());
        updateValue(target.getType(), source.getType());
        updateValue(target.getName(), source.getName());
    }

    public static String getServiceDeclarationNode(Service service) {
        StringBuilder builder = new StringBuilder();
        builder.append("service ");
        if (Objects.nonNull(service.getServiceType()) && service.getServiceType().isEnabled()) {
            builder.append(service.getServiceTypeName());
            builder.append(" ");
        }
        if (Objects.nonNull(service.getBasePath()) && service.getBasePath().isEnabled()) {
            builder.append(getValueString(service.getBasePath()));
            builder.append(" ");
        }
        builder.append("on ");
        if (Objects.nonNull(service.getListener()) && service.getListener().isEnabled()) {
            builder.append(service.getListener().getValue());
        }
        builder.append(" {");
        builder.append(System.lineSeparator());
        List<String> functions = new ArrayList<>();
        service.getFunctions().forEach(function -> {
            if (function.isEnabled()) {
                String functionNode = "\t" + getFunction(function).replace(System.lineSeparator(),
                        System.lineSeparator() + "\t");
                functions.add(functionNode);
            }
        });
        builder.append(String.join(System.lineSeparator() + System.lineSeparator(), functions));
        builder.append(System.lineSeparator());
        builder.append("}");
        return builder.toString();
    }

    public static String getValueString(Value value) {
        if (Objects.isNull(value)) {
            return "";
        }
        if (!value.isEnabled()) {
            return "";
        }
        if (!value.getValue().trim().isEmpty()) {
            return value.getValueType().equals("STRING") ? String.format("\"%s\"", value.getValue()) : value.getValue();
        }
        if (!value.getPlaceholder().trim().isEmpty()) {
            return value.getValueType().equals("STRING") ? String.format("\"%s\"", value.getPlaceholder()) :
                    value.getPlaceholder();
        }
        Map<String, Value> properties = value.getProperties();
        if (Objects.isNull(properties)) {
            return "";
        }
        List<String> params = new ArrayList<>();
        properties.forEach((key, val) -> {
            if (val.isEnabled()) {
                params.add(String.format("%s: %s", key, getValueString(val)));
            }
        });
        return String.format("{%s}", String.join(", ", params));
    }

    public static String getListenerParams(Value listener) {
        Map<String, Value> properties = listener.getProperties();
        List<String> params = new ArrayList<>();
        properties.forEach((key, value) -> {
            if (value.isEnabled()) {
                params.add(String.format("%s = %s", key, getValueString(value)));
            }
        });
        return String.join(", ", params);
    }

    public static String getFunction(Function function) {
        StringBuilder builder = new StringBuilder();
        String functionQualifiers = getFunctionQualifiers(function);
        if (!functionQualifiers.isEmpty()) {
            builder.append(functionQualifiers);
            builder.append(" ");
        }
        builder.append("function ");
        if (function.getKind().equals("RESOURCE") && Objects.nonNull(function.getAccessor())
                && function.getAccessor().isEnabled()) {
            builder.append(getValueString(function.getAccessor()).toLowerCase(Locale.ROOT));
            builder.append(" ");
        }
        builder.append(getValueString(function.getName()));
        builder.append(getFunctionSignature(function));
        builder.append("{");
        builder.append(System.lineSeparator());
        builder.append("\tdo {");
        builder.append(System.lineSeparator());
        builder.append("\t} on fail error err {");
        builder.append(System.lineSeparator());
        builder.append("\t\t// handle error");
        builder.append(System.lineSeparator());
        builder.append("\t}");
        builder.append(System.lineSeparator());
        builder.append("}");
        return builder.toString();
    }

    public static String getFunctionSignature(Function function) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        List<String> params = new ArrayList<>();
        function.getParameters().forEach(param -> {
            if (param.isEnabled()) {
                params.add(String.format("%s %s", getValueString(param.getType()), getValueString(param.getName())));
            }
        });
        builder.append(String.join(", ", params));
        builder.append(")");
        Value returnType = function.getReturnType();
        if (Objects.nonNull(returnType) && returnType.isEnabled()) {
            builder.append(" returns ");
            builder.append(getValueString(returnType));
        }
        builder.append(" ");
        return builder.toString();
    }

    public static String getFunctionQualifiers(Function function) {
        List<String> qualifiers = function.getQualifiers();
        qualifiers = Objects.isNull(qualifiers) ? new ArrayList<>() : qualifiers;
        String kind = function.getKind();
        switch (kind) {
            case "REMOTE" -> qualifiers.add("remote");
            case "RESOURCE" -> qualifiers.add("resource");
            default -> {
            }
        }
        return String.join(" ", qualifiers);
    }

    public static boolean importExists(ModulePartNode node, String org, String module) {
        return node.imports().stream().anyMatch(importDeclarationNode -> {
            String moduleName = importDeclarationNode.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            return importDeclarationNode.orgName().isPresent() &&
                    org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                    module.equals(moduleName);
        });
    }

    public static boolean filterTriggers(TriggerProperty triggerProperty, TriggerListRequest request) {
        return (request == null) ||
                ((request.organization() == null || request.organization().equals(triggerProperty.orgName())) &&
                (request.packageName() == null || request.packageName().equals(triggerProperty.packageName())) &&
                (request.keyWord() == null || triggerProperty.keywords().stream()
                        .anyMatch(keyword -> keyword.equalsIgnoreCase(request.keyWord()))) &&
                (request.query() == null || triggerProperty.keywords().stream()
                        .anyMatch(keyword -> keyword.contains(request.query()))));
    }

    public static boolean expectsTriggerByName(TriggerRequest request) {
        return request.id() == null && request.organization() != null && request.packageName() != null;
    }

    public static Optional<Listener> getListenerModel(ListenerDeclarationNode listenerDeclarationNode) {
        Optional<TypeDescriptorNode> typeDescriptorNode = listenerDeclarationNode.typeDescriptor();
        if (typeDescriptorNode.isEmpty() ||
                !typeDescriptorNode.get().kind().equals(SyntaxKind.QUALIFIED_NAME_REFERENCE)) {
            return Optional.empty();
        }
        String listenerProtocol = ((QualifiedNameReferenceNode) typeDescriptorNode.get()).modulePrefix().text().trim();
        Node initializer = listenerDeclarationNode.initializer();
        if (!initializer.kind().equals(SyntaxKind.IMPLICIT_NEW_EXPRESSION)) {
            return Optional.empty();
        }
        ImplicitNewExpressionNode newExpressionNode = (ImplicitNewExpressionNode) initializer;
        Map<String, Value> properties = new HashMap<>();
        newExpressionNode.parenthesizedArgList().ifPresent(argList -> {
            argList.arguments().forEach(arg -> {
                if (arg instanceof NamedArgumentNode namedArgumentNode) {
                    Value value = new Value();
                    value.setValue(namedArgumentNode.expression().toString().trim());
                    value.setEnabled(true);
                    value.setValueType("EXPRESSION");
                    properties.put(namedArgumentNode.argumentName().name().text().trim(), value);
                }
            });
        });
        Value nameValue = new Value();
        nameValue.setEnabled(true);
        nameValue.setValueType("EXPRESSION");
        nameValue.setValue(listenerDeclarationNode.variableName().text().trim());
        properties.put("name", nameValue);
        return Optional.of(new Listener(null, null, null, null, null, null, null, null, null, null, listenerProtocol,
                null, properties));
    }
}
