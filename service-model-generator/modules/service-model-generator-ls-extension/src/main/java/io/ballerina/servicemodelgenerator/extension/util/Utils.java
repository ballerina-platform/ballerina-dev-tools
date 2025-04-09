/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.servicemodelgenerator.extension.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.HttpResponse;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.TriggerProperty;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.TriggerListRequest;
import io.ballerina.servicemodelgenerator.extension.request.TriggerRequest;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.common.utils.NameUtil;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.GET;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_DEFAULT;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_DEFAULTABLE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_MUTATION;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_QUERY;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_REMOTE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_REQUIRED;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.KIND_SUBSCRIPTION;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.NEW_LINE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.REMOTE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.RESOURCE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.SPACE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.SUBSCRIBE;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION;
import static io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER;
import static io.ballerina.servicemodelgenerator.extension.util.HttpUtil.getHttpParameterType;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil.ServiceClassContext.GRAPHQL_DIAGRAM;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil.ServiceClassContext.HTTP_DIAGRAM;
import static io.ballerina.servicemodelgenerator.extension.util.ServiceClassUtil.ServiceClassContext.SERVICE_DIAGRAM;

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

    public static void populateRequiredFuncsDesignApproachAndServiceType(Service service) {
        populateRequiredFunctions(service);
        populateServiceType(service);
        populateDesignApproach(service);
    }

    public static void populateRequiredFunctions(Service service) {
        Value value = service.getProperty(ServiceModelGeneratorConstants.PROPERTY_REQUIRED_FUNCTIONS);
        if (Objects.nonNull(value) && value.isEnabledWithValue()) {
            String requiredFunction = value.getValue();
            service.getFunctions()
                    .forEach(function -> function.setEnabled(
                            function.getName().getValue().equals(requiredFunction)));
        }
    }

    private static void populateServiceType(Service service) {
        Value serviceValue = service.getServiceType();
        if (Objects.nonNull(serviceValue) && serviceValue.isEnabledWithValue()) {
            String serviceType = service.getServiceTypeName();
            if (Objects.nonNull(serviceType)) {
                getServiceByServiceType(serviceType.toLowerCase(Locale.ROOT))
                        .ifPresent(serviceTypeModel -> service.setFunctions(serviceTypeModel.getFunctions()));
            }
        }
    }

    public static void populateDesignApproach(Service service) {
        Value designApproach = service.getDesignApproach();
        if (Objects.nonNull(designApproach) && designApproach.isEnabled()
                && Objects.nonNull(designApproach.getChoices()) && !designApproach.getChoices().isEmpty()) {
            designApproach.getChoices().stream()
                    .filter(Value::isEnabled).findFirst()
                    .ifPresent(selectedApproach -> service.addProperties(selectedApproach.getProperties()));
            service.getProperties().remove(ServiceModelGeneratorConstants.PROPERTY_DESIGN_APPROACH);
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

    public static Optional<Symbol> getHttpServiceContractSym(SemanticModel semanticModel,
                                                             TypeDescriptorNode serviceTypeDesc) {
        Optional<Symbol> svcTypeSymbol = semanticModel.symbol(serviceTypeDesc);
        if (svcTypeSymbol.isEmpty() || !(svcTypeSymbol.get() instanceof TypeReferenceTypeSymbol svcTypeRef)) {
            return Optional.empty();
        }
        Optional<Symbol> contractSymbol = semanticModel.types().getTypeByName("ballerina", "http", "",
                "ServiceContract");
        if (contractSymbol.isEmpty() || !(contractSymbol.get() instanceof TypeDefinitionSymbol contractTypeDef)) {
            return Optional.empty();
        }
        if (svcTypeRef.subtypeOf(contractTypeDef.typeDescriptor())) {
            return svcTypeSymbol;
        }
        return Optional.empty();
    }

    public static String getPath(NodeList<Node> paths) {
        return paths.stream().map(Node::toString).map(String::trim).collect(Collectors.joining(""));
    }

    public static Function getFunctionModel(MethodDeclarationNode functionDefinitionNode, SemanticModel semanticModel,
                                            boolean isHttp, boolean isGraphQL, Map<String, Value> annotations) {
        boolean isInit = isInitFunction(functionDefinitionNode);
        ServiceClassUtil.ServiceClassContext context = deriveContext(isGraphQL, isHttp, isInit);
        Function functionModel = Function.getNewFunctionModel(context);
        functionModel.setAnnotations(annotations);

        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.methodName().text().trim());
        functionName.setValueType(VALUE_TYPE_IDENTIFIER);

        Value accessor = functionModel.getAccessor();
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            String qualifierText = qualifier.text().trim();
            if (qualifierText.matches(REMOTE)) {
                functionModel.setKind(KIND_REMOTE);
            } else if (qualifierText.matches(RESOURCE)) {
                functionModel.setKind(KIND_RESOURCE);
                accessor.setValue(functionDefinitionNode.methodName().text().trim());
                functionName.setValue(getPath(functionDefinitionNode.relativeResourcePath()));
            }
        }
        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.methodSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            returnType.setValue(returnTypeDesc.get().type().toString().trim());
            if (isHttp) {
                populateHttpResponses(functionDefinitionNode, returnType, semanticModel);
            }
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = getParameterModel(parameterNode, isHttp, isGraphQL);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        return functionModel;
    }

    public static Function getFunctionModel(FunctionDefinitionNode functionDefinitionNode, SemanticModel semanticModel,
                                            boolean isHttp, boolean isGraphQL, Map<String, Value> annotations) {
        boolean isInit = isInitFunction(functionDefinitionNode);
        ServiceClassUtil.ServiceClassContext context = deriveContext(isGraphQL, isHttp, isInit);

        Function functionModel = Function.getNewFunctionModel(context);
        functionModel.setAnnotations(annotations);

        if (isInit) {
            functionModel.setKind(KIND_DEFAULT);
        }

        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.functionName().text().trim());
        functionName.setValueType(VALUE_TYPE_IDENTIFIER);

        Value accessor = functionModel.getAccessor();
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            String qualifierText = qualifier.text().trim();
            if (qualifierText.matches(REMOTE)) {
                functionModel.setKind(isGraphQL ? KIND_MUTATION : KIND_REMOTE);
                break;
            } else if (qualifierText.matches(RESOURCE)) {
                if (isGraphQL) {
                    functionModel.setKind(functionName.getValue().equals(SUBSCRIBE) ? KIND_SUBSCRIPTION : KIND_QUERY);
                } else {
                    functionModel.setKind(KIND_RESOURCE);
                }
                accessor.setValue(functionDefinitionNode.functionName().text().trim());
                functionName.setValue(getPath(functionDefinitionNode.relativeResourcePath()));
                break;
            }
        }

        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            returnType.setValue(returnTypeDesc.get().type().toString().trim());
            if (isHttp) {
                populateHttpResponses(functionDefinitionNode, returnType, semanticModel);
            }
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = getParameterModel(parameterNode, isHttp, isGraphQL);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        functionModel.setCanAddParameters(true);
        updateAnnotationAttachmentProperty(functionDefinitionNode, functionModel);
        return functionModel;
    }

    private static ServiceClassUtil.ServiceClassContext deriveContext(boolean isGraphQL, boolean isHttp,
                                                                      boolean isInit) {
        if (isGraphQL && !isInit) {
            return GRAPHQL_DIAGRAM;
        } else if (isHttp && isInit) {
            return HTTP_DIAGRAM;
        }
        return SERVICE_DIAGRAM;
    }

    private static boolean isInitFunction(FunctionDefinitionNode functionDefinitionNode) {
        return functionDefinitionNode.functionName().text().trim().equals(ServiceModelGeneratorConstants.INIT);
    }

    private static boolean isInitFunction(MethodDeclarationNode functionDefinitionNode) {
        return functionDefinitionNode.methodName().text().trim().equals(ServiceModelGeneratorConstants.INIT);
    }

    private static void populateHttpResponses(MethodDeclarationNode functionDefinitionNode,
                                              FunctionReturnType returnType, SemanticModel semanticModel) {
        Optional<Symbol> functionDefSymbol = semanticModel.symbol(functionDefinitionNode);
        if (functionDefSymbol.isEmpty() || !(functionDefSymbol.get() instanceof ResourceMethodSymbol resource)) {
            return;
        }
        HttpUtil.populateHttpResponses(returnType, semanticModel, resource);
    }

    private static void populateHttpResponses(FunctionDefinitionNode functionDefinitionNode,
                                              FunctionReturnType returnType, SemanticModel semanticModel) {
        Optional<Symbol> functionDefSymbol = semanticModel.symbol(functionDefinitionNode);
        if (functionDefSymbol.isEmpty() || !(functionDefSymbol.get() instanceof ResourceMethodSymbol resource)) {
            return;
        }
        HttpUtil.populateHttpResponses(returnType, semanticModel, resource);
    }

    public static Optional<Parameter> getParameterModel(ParameterNode parameterNode, boolean isHttp,
                                                        boolean isGraphQL) {
        if (parameterNode instanceof RequiredParameterNode parameter) {
            if (parameter.paramName().isEmpty()) {
                return Optional.empty();
            }
            String paramName = parameter.paramName().get().text().trim();
            Parameter parameterModel = createParameter(paramName, KIND_REQUIRED, parameter.typeName().toString().trim(),
                    parameter.annotations(), isHttp, isGraphQL);
            return Optional.of(parameterModel);
        } else if (parameterNode instanceof DefaultableParameterNode parameter) {
            if (parameter.paramName().isEmpty()) {
                return Optional.empty();
            }
            String paramName = parameter.paramName().get().text().trim();
            Parameter parameterModel = createParameter(paramName, KIND_DEFAULTABLE,
                    parameter.typeName().toString().trim(), parameter.annotations(), isHttp, isGraphQL);
            Value defaultValue = parameterModel.getDefaultValue();
            defaultValue.setValue(parameter.expression().toString().trim());
            defaultValue.setValueType(VALUE_TYPE_EXPRESSION);
            defaultValue.setEnabled(true);
            return Optional.of(parameterModel);
        }
        return Optional.empty();
    }


    private static Parameter createParameter(String paramName, String paramKind, String typeName,
                                             NodeList<AnnotationNode> annotationNodes, boolean isHttp,
                                             boolean isGraphQL) {
        Parameter parameterModel = Parameter.getNewParameter(isGraphQL);
        parameterModel.setMetadata(new MetaData(paramName, paramName));
        parameterModel.setKind(paramKind);
        parameterModel.getType().setValue(typeName);
        parameterModel.getName().setValue(paramName);

        if (isHttp) {
            Optional<String> httpParameterType = getHttpParameterType(annotationNodes);
            if (httpParameterType.isPresent()) {
                parameterModel.setHttpParamType(httpParameterType.get());
            } else {
                if (!(typeName.equals("http:Request") || typeName.equals("http:Caller")
                        || typeName.equals("http:Headers") || typeName.equals("http:RequestContext"))) {
                    parameterModel.setHttpParamType(ServiceModelGeneratorConstants.HTTP_PARAM_TYPE_QUERY);
                    parameterModel.setEditable(true);
                }
            }
        }

        return parameterModel;
    }

    public static Optional<String> getPath(TypeDefinitionNode serviceTypeNode) {
        Optional<MetadataNode> metadata = serviceTypeNode.metadata();
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        Optional<AnnotationNode> httpServiceConfig = metadata.get().annotations().stream()
                .filter(annotation -> annotation.annotReference().toString().trim().equals(
                        ServiceModelGeneratorConstants.TYPE_HTTP_SERVICE_CONFIG))
                .findFirst();
        if (httpServiceConfig.isEmpty()) {
            return Optional.empty();
        }
        Optional<MappingConstructorExpressionNode> mapExpr = httpServiceConfig.get().annotValue();
        if (mapExpr.isEmpty()) {
            return Optional.empty();
        }
        Optional<SpecificFieldNode> basePathField = mapExpr.get().fields().stream()
                .filter(fieldNode -> fieldNode.kind().equals(SyntaxKind.SPECIFIC_FIELD))
                .map(fieldNode -> (SpecificFieldNode) fieldNode)
                .filter(fieldNode -> fieldNode.fieldName().toString().trim()
                        .equals(ServiceModelGeneratorConstants.BASE_PATH))
                .findFirst();
        if (basePathField.isEmpty()) {
            return Optional.empty();
        }
        Optional<ExpressionNode> valueExpr = basePathField.get().valueExpr();
        if (valueExpr.isPresent() && valueExpr.get().kind().equals(SyntaxKind.STRING_LITERAL)) {
            String value = ((BasicLiteralNode) valueExpr.get()).literalToken().text();
            return Optional.of(value.substring(1, value.length() - 1));
        }
        return Optional.empty();
    }

    public static Optional<Function> getFunctionModel(String serviceType, String functionNameOrType) {
        String resourcePath =  String.format("functions/%s_%s.json", serviceType.toLowerCase(Locale.US),
                functionNameOrType.toLowerCase(Locale.US));
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream(resourcePath);
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Function.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static void populateListenerInfo(Service serviceModel, ServiceDeclarationNode serviceNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        int size = expressions.size();
        if (size == 1) {
            serviceModel.getListener().setValue(getListenerExprName(expressions.get(0)));
        } else if (size > 1) {
            for (int i = 0; i < size; i++) {
                ExpressionNode expressionNode = expressions.get(i);
                serviceModel.getListener().addValue(getListenerExprName(expressionNode));
            }
        }
    }

    public static void updateAnnotationAttachmentProperty(ServiceDeclarationNode serviceNode,
                                                          Service service) {
        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) {
            return;
        }

        metadata.get().annotations().forEach(annotationNode -> {
            if (annotationNode.annotValue().isEmpty()) {
                return;
            }
            String annotName = annotationNode.annotReference().toString().trim();
            String[] split = annotName.split(":");
            annotName = split[split.length - 1];
            String propertyName = "annot" + annotName;
            if (service.getProperties().containsKey(propertyName)) {
                Value property = service.getProperties().get(propertyName);
                property.setValue(annotationNode.annotValue().get().toSourceCode().trim());
            }
        });
    }

    public static void updateAnnotationAttachmentProperty(FunctionDefinitionNode functionDef,
                                                          Function function) {
        Optional<MetadataNode> metadata = functionDef.metadata();
        if (metadata.isEmpty()) {
            return;
        }

        metadata.get().annotations().forEach(annotationNode -> {
            if (annotationNode.annotValue().isEmpty()) {
                return;
            }
            String annotName = annotationNode.annotReference().toString().trim();
            String[] split = annotName.split(":");
            annotName = split[split.length - 1];
            String propertyName = "annot" + annotName;
            if (function.getAnnotations().containsKey(propertyName)) {
                Value property = function.getAnnotations().get(propertyName);
                property.setValue(annotationNode.annotValue().get().toSourceCode().trim());
            }
        });
    }

    private static String getListenerExprName(ExpressionNode expressionNode) {
        if (expressionNode instanceof NameReferenceNode nameReferenceNode) {
            return nameReferenceNode.toSourceCode().trim();
        } else if (expressionNode instanceof ExplicitNewExpressionNode explicitNewExpressionNode) {
            return explicitNewExpressionNode.toSourceCode().trim();
        }
        return "";
    }

    public static boolean isPresent(Function functionModel, Function newFunction) {
        return newFunction.getName().getValue().equals(functionModel.getName().getValue()) &&
                (Objects.isNull(newFunction.getAccessor()) || Objects.isNull(functionModel.getAccessor()) ||
                        newFunction.getAccessor().getValue().equals(functionModel.getAccessor().getValue()));
    }

    public static void updateValue(Value target, Value source) {
        if (Objects.isNull(target) || Objects.isNull(source)) {
            return;
        }
        target.setEnabled(source.isEnabledWithValue());
        target.setValue(source.getValue());
        target.setValueType(source.getValueType());
    }

    public static void updateValue(FunctionReturnType target, FunctionReturnType source) {
        if (Objects.isNull(target) || Objects.isNull(source)) {
            return;
        }
        target.setEnabled(source.isEnabledWithValue());
        target.setValue(source.getValue());
        target.setValueType(source.getValueType());
        if (Objects.nonNull(source.getResponses())) {
            target.setResponses(source.getResponses());
        }
    }

    public static String getServiceDeclarationNode(Service service, FunctionAddContext context,
                                                   Map<String, String> imports) {
        StringBuilder builder = new StringBuilder();
        List<String> annots = getAnnotationEdits(service);

        if (!annots.isEmpty()) {
            builder.append(String.join(NEW_LINE, annots));
            builder.append(System.lineSeparator());
        }

        builder.append(ServiceModelGeneratorConstants.SERVICE).append(ServiceModelGeneratorConstants.SPACE);
        if (Objects.nonNull(service.getServiceType()) && service.getServiceType().isEnabledWithValue()) {
            builder.append(service.getServiceTypeName());
            builder.append(ServiceModelGeneratorConstants.SPACE);
        }
        if (Objects.nonNull(service.getServiceContractTypeNameValue()) &&
                service.getServiceContractTypeNameValue().isEnabledWithValue()) {
            builder.append(service.getServiceContractTypeName());
            builder.append(ServiceModelGeneratorConstants.SPACE);
        } else if (Objects.nonNull(service.getBasePath()) && service.getBasePath().isEnabledWithValue()) {
            builder.append(getValueString(service.getBasePath()));
            builder.append(ServiceModelGeneratorConstants.SPACE);
        } else if (Objects.nonNull(service.getStringLiteralProperty()) &&
                service.getStringLiteralProperty().isEnabledWithValue()) {
            builder.append(getValueString(service.getStringLiteralProperty()));
            builder.append(ServiceModelGeneratorConstants.SPACE);
        }

        builder.append(ServiceModelGeneratorConstants.ON).append(ServiceModelGeneratorConstants.SPACE);
        if (Objects.nonNull(service.getListener()) && service.getListener().isEnabledWithValue()) {
            builder.append(service.getListener().getValue());
        }
        builder.append(ServiceModelGeneratorConstants.SPACE).append(ServiceModelGeneratorConstants.OPEN_BRACE);
        builder.append(System.lineSeparator());
        List<String> functions = new ArrayList<>();
        boolean isNewTcpService = Utils.isTcpService(service.getOrgName(), service.getPackageName())
                && service.getProperties().containsKey("returningServiceClass");

        boolean isAiAgent = Utils.isAiAgentModule(service.getOrgName(), service.getPackageName());

        if (isNewTcpService) {
            String serviceClassName = service.getProperties().get("returningServiceClass").getValue();
            String onConnectFunc = Utils.getTcpOnConnectTemplate().formatted(serviceClassName, serviceClassName);
            functions.add(onConnectFunc);
        } else if (isAiAgent) {
            String chatFunction = getAgentChatFunction();
            functions.add(chatFunction);
        } else {
            service.getFunctions().forEach(function -> {
                if (function.isEnabled()) {
                    String functionNode = "\t" + generateFunctionDefSource(function, new ArrayList<>(), context,
                            FunctionSignatureContext.FUNCTION_ADD, imports)
                            .replace(System.lineSeparator(), System.lineSeparator() + "\t");
                    functions.add(functionNode);
                }
            });
        }
        builder.append(String.join(System.lineSeparator() + System.lineSeparator(), functions));
        builder.append(System.lineSeparator());
        builder.append(ServiceModelGeneratorConstants.CLOSE_BRACE);
        return builder.toString();
    }

    private static String getAgentChatFunction() {
        return "    resource function post chat(@http:Payload ai:ChatReqMessage request) " +
                "returns ai:ChatRespMessage|error {" + System.lineSeparator() +
                "    }";
    }

    public static List<String> getAnnotationEdits(Service service) {
        Map<String, Value> properties = service.getProperties();
        List<String> annots = new ArrayList<>();
        for (Map.Entry<String, Value> property : properties.entrySet()) {
            Value value = property.getValue();
            if (Objects.nonNull(value.getCodedata()) && Objects.nonNull(value.getCodedata().getType()) &&
                    value.getCodedata().getType().equals("ANNOTATION_ATTACHMENT") && value.isEnabledWithValue()) {
                String ref = service.getModuleName() + ":" + value.getCodedata().getOriginalName();
                String annotTemplate = "@%s%s".formatted(ref, value.getValue());
                annots.add(annotTemplate);
            }
        }
        return annots;
    }

    public static List<String> getAnnotationEdits(Function function) {
        Map<String, Value> properties = function.getAnnotations();
        List<String> annots = new ArrayList<>();
        for (Map.Entry<String, Value> property : properties.entrySet()) {
            Value value = property.getValue();
            if (Objects.nonNull(value.getCodedata()) && Objects.nonNull(value.getCodedata().getType()) &&
                    value.getCodedata().getType().equals("ANNOTATION_ATTACHMENT") && value.isEnabledWithValue()) {
                Codedata codedata = value.getCodedata();
                String ref = codedata.getModuleName() + ":" + codedata.getOriginalName();
                String annotTemplate = "@%s%s".formatted(ref, value.getValue());
                annots.add(annotTemplate);
            }
        }
        return annots;
    }

    public static int addServiceAnnotationTextEdits(Service service, ServiceDeclarationNode serviceNode,
                                                    List<TextEdit> edits) {
        Token serviceKeyword = serviceNode.serviceKeyword();

        List<String> annots = getAnnotationEdits(service);
        String annotEdit = String.join(System.lineSeparator(), annots);

        Optional<MetadataNode> metadata = serviceNode.metadata();
        if (metadata.isEmpty()) { // metadata is empty and service model has annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(serviceKeyword.lineRange().startLine()), annotEdit));
            }
            return annots.size();
        }
        NodeList<AnnotationNode> annotations = metadata.get().annotations();
        if (annotations.isEmpty()) { // metadata is present but no annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(metadata.get().lineRange()), annotEdit));
            }
            return annots.size();
        }

        // first annotation end line range
        int size = annotations.size();
        LinePosition firstAnnotationEndLinePos = annotations.get(0).lineRange().startLine();

        // last annotation end line range
        LinePosition lastAnnotationEndLinePos = annotations.get(size - 1).lineRange().endLine();

        LineRange range = LineRange.from(serviceKeyword.lineRange().fileName(),
                firstAnnotationEndLinePos, lastAnnotationEndLinePos);

        if (!annotEdit.isEmpty()) {
            edits.add(new TextEdit(toRange(range), annotEdit));
        }

        return annots.size();
    }

    public static int addFunctionAnnotationTextEdits(Function function, FunctionDefinitionNode functionDef,
                                                    List<TextEdit> edits) {
        Token firstToken = functionDef.qualifierList().isEmpty() ? functionDef.functionKeyword()
                : functionDef.qualifierList().get(0);

        List<String> annots = getAnnotationEdits(function);
        String annotEdit = String.join(System.lineSeparator(), annots);

        Optional<MetadataNode> metadata = functionDef.metadata();
        if (metadata.isEmpty()) { // metadata is empty and service model has annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(firstToken.lineRange().startLine()), annotEdit));
            }
            return annots.size();
        }
        NodeList<AnnotationNode> annotations = metadata.get().annotations();
        if (annotations.isEmpty()) { // metadata is present but no annotations
            if (!annotEdit.isEmpty()) {
                annotEdit += System.lineSeparator();
                edits.add(new TextEdit(toRange(metadata.get().lineRange()), annotEdit));
            }
            return annots.size();
        }

        // first annotation end line range
        int size = annotations.size();
        LinePosition firstAnnotationEndLinePos = annotations.get(0).lineRange().startLine();

        // last annotation end line range
        LinePosition lastAnnotationEndLinePos = annotations.get(size - 1).lineRange().endLine();

        LineRange range = LineRange.from(firstToken.lineRange().fileName(),
                firstAnnotationEndLinePos, lastAnnotationEndLinePos);

        if (!annotEdit.isEmpty()) {
            edits.add(new TextEdit(toRange(range), annotEdit));
        }

        return annots.size();
    }

    public static String getValueString(Value value) {
        if (Objects.isNull(value)) {
            return "";
        }
        if (!value.isEnabledWithValue()) {
            return "";
        }
        if (!value.getValue().trim().isEmpty()) {
            return !Objects.isNull(value.getValueType()) && value.getValueType().equals("STRING") ?
                    String.format("\"%s\"", value.getValue()) : value.getValue();
        }
        Map<String, Value> properties = value.getProperties();
        if (Objects.isNull(properties)) {
            return "";
        }
        List<String> params = new ArrayList<>();
        properties.forEach((key, val) -> {
            if (val.isEnabledWithValue()) {
                params.add(String.format("%s: %s", key, getValueString(val)));
            }
        });
        return String.format("{%s}", String.join(", ", params));
    }

    public enum FunctionAddContext {
        HTTP_SERVICE_ADD,
        TCP_SERVICE_ADD,
        GRAPHQL_SERVICE_ADD,
        TRIGGER_ADD,
        FUNCTION_ADD,
        RESOURCE_ADD
    }

    public enum FunctionSignatureContext {
        FUNCTION_ADD,
        HTTP_RESOURCE_ADD,
        FUNCTION_UPDATE
    }

    public static String generateFunctionDefSource(Function function, List<String> statusCodeResponses,
                                                   FunctionAddContext addContext,
                                                   FunctionSignatureContext signatureContext,
                                                   Map<String, String> imports) {
        StringBuilder builder = new StringBuilder();

        List<String> functionAnnotations = getAnnotationEdits(function);
        if (!functionAnnotations.isEmpty()) {
            builder.append(String.join(NEW_LINE, functionAnnotations)).append(NEW_LINE);
        }

        String functionQualifiers = getFunctionQualifiers(function);
        if (!functionQualifiers.isEmpty()) {
            builder.append(functionQualifiers).append(SPACE);
        }
        builder.append("function ");

        // function accessor
        Value accessor = function.getAccessor();
        if (function.getKind().equals(KIND_RESOURCE) && Objects.nonNull(accessor) && accessor.isEnabledWithValue()) {
            builder.append(getValueString(accessor).toLowerCase(Locale.ROOT)).append(SPACE);
        }
        if (function.getKind().equals(KIND_SUBSCRIPTION)) {
            builder.append(SUBSCRIBE).append(SPACE);
        }
        if (function.getKind().equals(KIND_QUERY)) {
            builder.append(GET).append(SPACE);
        }

        // function identifier
        builder.append(getValueString(function.getName()));

        FunctionSignatureContext sigContext = addContext.equals(FunctionAddContext.HTTP_SERVICE_ADD) ?
                FunctionSignatureContext.HTTP_RESOURCE_ADD : signatureContext;
        String functionSignature = generateFunctionSignatureSource(function, statusCodeResponses, sigContext, imports);
        builder.append(functionSignature);

        FunctionReturnType returnType = function.getReturnType();

        boolean hasErrorInReturn = returnType.hasError() || addContext.equals(FunctionAddContext.HTTP_SERVICE_ADD) ||
                signatureContext.equals(FunctionSignatureContext.HTTP_RESOURCE_ADD);

        if (!hasErrorInReturn && Objects.nonNull(returnType.getValue())) {
            List<String> returnParts = Arrays.stream(returnType.getValue().split("\\|")).toList();
            hasErrorInReturn = returnParts.contains("error") || returnParts.contains("error?");
        }


        // function body
        builder.append("{").append(NEW_LINE);
        if (hasErrorInReturn) {
            builder.append("\tdo {").append(NEW_LINE);
            if (addContext.equals(FunctionAddContext.HTTP_SERVICE_ADD)) {
                builder.append("\t\treturn \"Hello, Greetings!\";").append(NEW_LINE);
            }
            builder.append("\t} on fail error err {")
                    .append(NEW_LINE)
                    .append("\t\t// handle error")
                    .append(NEW_LINE)
                    .append("\t\treturn error(\"unhandled error\", err);")
                    .append(NEW_LINE)
                    .append("\t}")
                    .append(NEW_LINE);
        }
        builder.append("}");
        return builder.toString();
    }

    public static String generateFunctionSignatureSource(Function function, List<String> statusCodeResponses,
                                                         FunctionSignatureContext context,
                                                         Map<String, String> imports) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(generateFunctionParamListSource(function.getParameters(), imports));
        builder.append(")");

        FunctionReturnType returnType = function.getReturnType();
        boolean addError = context.equals(FunctionSignatureContext.HTTP_RESOURCE_ADD);
        if (Objects.nonNull(returnType)) {
            if (returnType.isEnabledWithValue()) {
                builder.append(" returns ");
                String returnTypeStr = getValueString(returnType);
                if (addError && !returnTypeStr.contains("error")) {
                    returnTypeStr = "error|" + returnTypeStr;
                }
                builder.append(returnTypeStr);
                if (Objects.nonNull(returnType.getImports())) {
                    imports.putAll(returnType.getImports());
                }
            } else if (returnType.isEnabled() && Objects.nonNull(returnType.getResponses()) &&
                    !returnType.getResponses().isEmpty()) {
                List<String> responses = new ArrayList<>(returnType.getResponses().stream()
                        .filter(HttpResponse::isEnabled)
                        .map(response -> HttpUtil.getStatusCodeResponse(response, statusCodeResponses, imports))
                        .filter(Objects::nonNull)
                        .toList());
                if (!responses.isEmpty()) {
                    if (addError && !statusCodeResponses.contains("error")) {
                        responses.addFirst("error");
                    }
                    builder.append(" returns ");
                    builder.append(String.join("|", responses));
                }
            }
        }
        builder.append(SPACE);
        return builder.toString();
    }

    private static String generateFunctionParamListSource(List<Parameter> parameters, Map<String, String> imports) {
        // sort params list where required params come first
        parameters.sort(new Parameter.RequiredParamSorter());

        List<String> params = new ArrayList<>();
        parameters.forEach(param -> {
            if (param.isEnabled()) {
                String paramDef;
                Value defaultValue = param.getDefaultValue();
                if (Objects.nonNull(defaultValue) && defaultValue.isEnabled() &&
                        Objects.nonNull(defaultValue.getValue()) && !defaultValue.getValue().isEmpty()) {
                    Value paramType = param.getType();
                    paramDef = String.format("%s %s = %s", getValueString(paramType), getValueString(param.getName()),
                            getValueString(defaultValue));
                    if (Objects.nonNull(paramType.getImports())) {
                        imports.putAll(paramType.getImports());
                    }
                } else {
                    Value paramType = param.getType();
                    if (Objects.nonNull(paramType.getImports())) {
                        imports.putAll(paramType.getImports());
                    }
                    paramDef = String.format("%s %s", getValueString(paramType), getValueString(param.getName()));
                }
                if (Objects.nonNull(param.getHttpParamType()) && !param.getHttpParamType().equals("Query")) {
                    paramDef = String.format("@http:%s %s", param.getHttpParamType(), paramDef);
                }
                params.add(paramDef);
            }
        });
        return String.join(", ", params);
    }

    public static String getFunctionQualifiers(Function function) {
        List<String> qualifiers = function.getQualifiers();
        qualifiers = Objects.isNull(qualifiers) ? new ArrayList<>() : qualifiers;
        String kind = function.getKind();
        switch (kind) {
            case KIND_QUERY, KIND_SUBSCRIPTION,
                 KIND_RESOURCE ->
                    qualifiers.add(RESOURCE);
            case KIND_REMOTE, KIND_MUTATION ->
                    qualifiers.add(REMOTE);

            default -> {
            }
        }
        return String.join(" ", qualifiers);
    }

    /**
     * Checks whether the given import exists in the given module part node.
     *
     * @param node module part node
     * @param org organization name
     * @param module module name
     * @return true if the import exists, false otherwise
     */
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

    /**
     * Generates the import statement for the given organization and module.
     *
     * @param org organization name
     * @param module module name
     * @return generated import statement
     */
    public static String getImportStmt(String org, String module) {
        return String.format(ServiceModelGeneratorConstants.IMPORT_STMT_TEMPLATE, org, module);
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

    public static boolean isTcpService(String org, String module) {
        return org.equals("ballerina") && module.equals("tcp");
    }

    public static String getTcpOnConnectTemplate() {
        return "    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService|tcp:Error? {%n" +
                "        do {%n" +
                "            %s connectionService = new %s();%n" +
                "            return connectionService;%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            return error(\"unhandled error\", err);%n" +
                "        }%n" +
                "    }";
    }

    public static FunctionAddContext getTriggerAddContext(String org, String module) {
        if (org.equals("ballerina")) {
            if (module.equals("http")) {
                return FunctionAddContext.HTTP_SERVICE_ADD;
            } else if (module.equals("graphql")) {
                return FunctionAddContext.GRAPHQL_SERVICE_ADD;
            } else if (module.equals("tcp")) {
                return FunctionAddContext.TCP_SERVICE_ADD;
            }
        }
        return FunctionAddContext.TRIGGER_ADD;
    }

    public static String generateVariableIdentifier(SemanticModel semanticModel, Document document,
                                                    LinePosition linePosition, String prefix) {
        Set<String> names = semanticModel.visibleSymbols(document, linePosition).parallelStream()
                .filter(s -> s.getName().isPresent())
                .map(s -> s.getName().get())
                .collect(Collectors.toSet());
        return NameUtil.generateVariableName(prefix, names);
    }

    public static String generateTypeIdentifier(SemanticModel semanticModel, Document document,
                                                    LinePosition linePosition, String prefix) {
        Set<String> names = semanticModel.visibleSymbols(document, linePosition).parallelStream()
                .filter(s -> s.getName().isPresent())
                .map(s -> s.getName().get())
                .collect(Collectors.toSet());
        return NameUtil.generateTypeName(prefix, names);
    }

    public static String upperCaseFirstLetter(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }

    public static String removeLeadingSingleQuote(String input) {
        if (input != null && input.startsWith("'")) {
            return input.substring(1);
        }
        return input;
    }

    public static boolean isAiAgentModule(String org, String module) {
        return org.equals("ballerinax") && module.equals("ai");
    }
}
