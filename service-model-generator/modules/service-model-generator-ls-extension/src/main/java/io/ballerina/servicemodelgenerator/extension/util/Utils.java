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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static void enableContractFirstApproach(Service service) {
        Value designApproach = service.getDesignApproach();
        if (Objects.nonNull(designApproach) && Objects.nonNull(designApproach.getChoices())
                && !designApproach.getChoices().isEmpty()) {
            designApproach.getChoices().forEach(choice -> choice.setEnabled(false));
            designApproach.getChoices().stream()
                    .filter(choice -> choice.getMetadata().label().equals("Import from OpenAPI Specification"))
                    .findFirst()
                    .ifPresent(approach -> {
                approach.setEnabled(true);
                approach.getProperties().remove("spec");
            });
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

    public static Service getServiceModel(TypeDefinitionNode serviceTypeNode,
                                          ServiceDeclarationNode serviceDeclarationNode,
                                          SemanticModel semanticModel, boolean isHttp) {
        Service serviceModel = Service.getNewService();
        Optional<String> basePath = getPath(serviceTypeNode);
        if (basePath.isPresent() && !basePath.get().isEmpty()) {
            MetaData metaData = new MetaData("Service Base Path", "The base path for the service");
            Value basePathValue = new Value();
            basePathValue.setValue(basePath.get());
            basePathValue.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
            basePathValue.setEnabled(true);
            basePathValue.setMetadata(metaData);
            serviceModel.setBasePath(basePathValue);
        }
        Value serviceType = new Value();
        serviceType.setValue(serviceTypeNode.typeName().text().trim());
        serviceType.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
        serviceType.setEnabled(true);
        serviceModel.setServiceContractTypeName(serviceType);
        List<Function> functionModels = new ArrayList<>();
        serviceDeclarationNode.members().forEach(member -> {
            if (member instanceof FunctionDefinitionNode functionDefinitionNode) {
                Function functionModel = getFunctionModel(functionDefinitionNode, semanticModel, isHttp,
                        false);
                functionModels.add(functionModel);
            }
        });
        serviceModel.setFunctions(functionModels);
        return serviceModel;
    }

    public static Service getServiceModel(ServiceDeclarationNode serviceDeclarationNode, SemanticModel semanticModel,
                                          boolean isHttp, boolean isGraphQL) {
        Service serviceModel = Service.getNewService();
        List<Function> functionModels = new ArrayList<>();
        serviceDeclarationNode.members().forEach(member -> {
            if (member instanceof FunctionDefinitionNode functionDefinitionNode) {
                Function functionModel = getFunctionModel(functionDefinitionNode, semanticModel, isHttp, isGraphQL);
                functionModels.add(functionModel);
            }
        });
        serviceModel.setFunctions(functionModels);
        return serviceModel;
    }

    public static boolean isHttpServiceContractType(SemanticModel semanticModel, TypeDescriptorNode serviceTypeDesc) {
        Optional<Symbol> svcTypeSymbol = semanticModel.symbol(serviceTypeDesc);
        if (svcTypeSymbol.isEmpty() || !(svcTypeSymbol.get() instanceof TypeReferenceTypeSymbol svcTypeRef)) {
            return false;
        }
        Optional<Symbol> contractSymbol = semanticModel.types().getTypeByName("ballerina", "http", "",
                "ServiceContract");
        if (contractSymbol.isEmpty() || !(contractSymbol.get() instanceof TypeDefinitionSymbol contractTypeDef)) {
            return false;
        }
        return svcTypeRef.subtypeOf(contractTypeDef.typeDescriptor());
    }

    public static String getPath(NodeList<Node> paths) {
        return paths.stream().map(Node::toString).map(String::trim).collect(Collectors.joining(""));
    }

    public static Function getFunctionModel(MethodDeclarationNode functionDefinitionNode, SemanticModel semanticModel,
                                            boolean isHttp, boolean isGraphQL) {
        boolean isInit = isInitFunction(functionDefinitionNode);
        ServiceClassUtil.ServiceClassContext context = deriveContext(isGraphQL, isHttp, isInit);
        Function functionModel = Function.getNewFunctionModel(context);
        functionModel.setEnabled(true);
        Value accessor = functionModel.getAccessor();
        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.methodName().text().trim());
        functionName.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
        functionName.setEnabled(true);
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            if (qualifier.text().trim().matches(ServiceModelGeneratorConstants.REMOTE)) {
                functionModel.setKind(ServiceModelGeneratorConstants.KIND_REMOTE);
            } else if (qualifier.text().trim().matches(ServiceModelGeneratorConstants.RESOURCE)) {
                functionModel.setKind(ServiceModelGeneratorConstants.KIND_RESOURCE);
                accessor.setValue(functionDefinitionNode.methodName().text().trim());
                accessor.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
                accessor.setEnabled(true);
                functionName.setValue(getPath(functionDefinitionNode.relativeResourcePath()));
            } else {
                functionModel.addQualifier(qualifier.text().trim());
            }
        }
        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.methodSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            if (Objects.nonNull(returnType)) {
                returnType.setValue(returnTypeDesc.get().type().toString().trim());
                returnType.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
                returnType.setEnabled(true);
            }
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

    public static Function getFunctionModel(FunctionDefinitionNode functionDefinitionNode,
                                            SemanticModel semanticModel, boolean isHttp, boolean isGraphQL) {
        boolean isInit = isInitFunction(functionDefinitionNode);
        ServiceClassUtil.ServiceClassContext context = deriveContext(isGraphQL, isHttp, isInit);
        Function functionModel = Function.getNewFunctionModel(context);
        functionModel.setEnabled(true);
        Value accessor = functionModel.getAccessor();
        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.functionName().text().trim());
        functionName.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
        functionName.setEnabled(true);
        if (isGraphQL) {
            accessor.setEditable(false);
        }
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            if (qualifier.text().trim().matches(ServiceModelGeneratorConstants.REMOTE)) {
                if (isGraphQL) {
                    functionModel.setKind(ServiceModelGeneratorConstants.KIND_MUTATION);
                } else {
                    functionModel.setKind(ServiceModelGeneratorConstants.KIND_REMOTE);
                }
            } else if (qualifier.text().trim().matches(ServiceModelGeneratorConstants.RESOURCE)) {
                if (isGraphQL) {
                    if (functionName.getValue().equals(ServiceModelGeneratorConstants.SUBSCRIBE)) {
                        functionModel.setKind(ServiceModelGeneratorConstants.KIND_SUBSCRIPTION);
                    } else {
                        functionModel.setKind(ServiceModelGeneratorConstants.KIND_QUERY);
                    }
                } else {
                    functionModel.setKind(ServiceModelGeneratorConstants.KIND_RESOURCE);
                }
                accessor.setValue(functionDefinitionNode.functionName().text().trim());
                accessor.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
                accessor.setEnabled(true);
                functionName.setValue(getPath(functionDefinitionNode.relativeResourcePath()));
            } else {
                functionModel.addQualifier(qualifier.text().trim());
            }
        }
        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            FunctionReturnType returnType = functionModel.getReturnType();
            if (Objects.nonNull(returnType)) {
                returnType.setValue(returnTypeDesc.get().type().toString().trim());
                returnType.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
                returnType.setEnabled(true);
            }
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

    public static Optional<String> getHttpParameterType(NodeList<AnnotationNode> annotations) {
        for (AnnotationNode annotation : annotations) {
            Node annotReference = annotation.annotReference();
            String annotName = annotReference.toString();
            if (annotReference.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE) {
                continue;
            }
            String[] annotStrings = annotName.split(":");
            if (!annotStrings[0].trim().equals(ServiceModelGeneratorConstants.HTTP)) {
                continue;
            }
            return Optional.of(annotStrings[annotStrings.length - 1].trim().toUpperCase(Locale.ROOT));
        }
        return Optional.empty();
    }

    public static Optional<Parameter> getParameterModel(ParameterNode parameterNode, boolean isHttp,
                                                        boolean isGraphQL) {
        if (parameterNode instanceof RequiredParameterNode parameter) {
            String paramName = parameter.paramName().get().toString().trim();
            Parameter parameterModel = createParameter(paramName, ServiceModelGeneratorConstants.KIND_REQUIRED,
                    ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER, parameter.typeName().toString().trim(),
                    parameter.annotations(), isHttp, isGraphQL);
            return Optional.of(parameterModel);
        } else if (parameterNode instanceof DefaultableParameterNode parameter) {
            String paramName = parameter.paramName().get().toString().trim();
            Parameter parameterModel = createParameter(paramName, ServiceModelGeneratorConstants.KIND_DEFAULTABLE,
                    ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION, parameter.typeName().toString().trim(),
                    parameter.annotations(), isHttp, isGraphQL);
            Value defaultValue = parameterModel.getDefaultValue();
            defaultValue.setValue(parameter.expression().toString().trim());
            defaultValue.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION);
            defaultValue.setEnabled(true);
            return Optional.of(parameterModel);
        }
        return Optional.empty();
    }


    private static Parameter createParameter(String paramName, String paramKind, String valueType, String typeName,
                                             NodeList<AnnotationNode> annotationNodes, boolean isHttp,
                                             boolean isGraphQL) {
        Parameter parameterModel = Parameter.getNewParameter(isGraphQL);
        parameterModel.setMetadata(new MetaData(paramName, paramName));
        parameterModel.setKind(paramKind);
        if (isHttp) {
            Optional<String> httpParameterType = getHttpParameterType(annotationNodes);
            if (httpParameterType.isPresent()) {
                parameterModel.setHttpParamType(httpParameterType.get());
            } else {
                parameterModel.setHttpParamType(ServiceModelGeneratorConstants.HTTP_PARAM_TYPE_QUERY);
            }
        }
        getHttpParameterType(annotationNodes).ifPresent(parameterModel::setHttpParamType);
        Value type = parameterModel.getType();
        type.setValue(typeName);
        type.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
        type.setType(true);
        type.setEnabled(true);
        Value name = parameterModel.getName();
        name.setValue(paramName);
        name.setValueType(valueType);
        name.setEnabled(true);
        parameterModel.setEnabled(true);
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

    public static void updateServiceContractModel(Service serviceModel, TypeDefinitionNode serviceTypeNode,
                                                  ServiceDeclarationNode serviceDeclaration,
                                                  SemanticModel semanticModel) {
        Service commonSvcModel = getServiceModel(serviceTypeNode, serviceDeclaration, semanticModel, true);

        if (Objects.nonNull(serviceModel.getServiceType()) && Objects.nonNull(commonSvcModel.getServiceType())) {
            serviceModel.updateServiceType(commonSvcModel.getServiceType());
        }
        updateServiceInfo(serviceModel, commonSvcModel);
        serviceModel.setCodedata(new Codedata(serviceDeclaration.lineRange()));
        populateListenerInfo(serviceModel, serviceDeclaration);
    }

    public static void updateServiceModel(Service serviceModel, ServiceDeclarationNode serviceNode,
                                          SemanticModel semanticModel) {
        String moduleName = serviceModel.getModuleName();
        boolean isHttp = moduleName.equals(ServiceModelGeneratorConstants.HTTP);
        boolean isGraphql = moduleName.equals(ServiceModelGeneratorConstants.GRAPHQL);
        Service commonSvcModel = getServiceModel(serviceNode, semanticModel, isHttp, isGraphql);
        updateServiceInfo(serviceModel, commonSvcModel);
        serviceModel.setCodedata(new Codedata(serviceNode.lineRange()));
        populateListenerInfo(serviceModel, serviceNode);

        // handle base path and string literal
        String attachPoint = getPath(serviceNode.absoluteResourcePath());
        if (!attachPoint.isEmpty()) {
            boolean isStringLiteral = attachPoint.startsWith("\"") && attachPoint.endsWith("\"");
            if (isStringLiteral) {
                Value stringLiteralProperty = serviceModel.getStringLiteralProperty();
                if (Objects.nonNull(stringLiteralProperty)) {
                    stringLiteralProperty.setValue(attachPoint);
                } else {
                    serviceModel.setStringLiteral(ServiceModelUtils.getStringLiteralProperty(attachPoint));
                }
            } else {
                Value basePathProperty = serviceModel.getBasePath();
                if (Objects.nonNull(basePathProperty)) {
                    basePathProperty.setValue(attachPoint);
                } else {
                    serviceModel.setBasePath(ServiceModelUtils.getBasePathProperty(attachPoint));
                }
            }
        }
    }

    private static void updateServiceInfo(Service serviceModel, Service commonSvcModel) {
        Value serviceContractTypeNameValue = commonSvcModel.getServiceContractTypeNameValue();
        if (Objects.nonNull(serviceContractTypeNameValue)) {
            enableContractFirstApproach(serviceModel);
        }
        populateRequiredFuncsDesignApproachAndServiceType(serviceModel);
        updateValue(serviceModel.getServiceContractTypeNameValue(), commonSvcModel.getServiceContractTypeNameValue());

        // mark the enabled functions as true if they present in the source
        serviceModel.getFunctions().forEach(functionModel -> {
            Optional<Function> function = commonSvcModel.getFunctions().stream()
                    .filter(newFunction -> isPresent(functionModel, newFunction)
                            && newFunction.getKind().equals(functionModel.getKind()))
                    .findFirst();
            function.ifPresentOrElse(
                    func -> updateFunction(functionModel, func, serviceModel),
                    () -> functionModel.setEnabled(false)
            );
        });

        // functions contains in source but not enforced using the service contract type
        commonSvcModel.getFunctions().forEach(functionModel -> {
            if (serviceModel.getFunctions().stream()
                    .noneMatch(newFunction -> isPresent(functionModel, newFunction))) {
                if (serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.HTTP) &&
                        functionModel.getKind().equals(ServiceModelGeneratorConstants.KIND_RESOURCE)) {
                    getResourceFunctionModel().ifPresentOrElse(
                            resourceFunction -> {
                                updateFunctionInfo(resourceFunction, functionModel);
                                serviceModel.addFunction(resourceFunction);
                            },
                            () -> serviceModel.addFunction(functionModel)
                    );
                } else if (serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.GRAPHQL)) {
                    GraphqlUtil.updateGraphqlFunctionMetaData(functionModel);
                    serviceModel.addFunction(functionModel);
                } else {
                    serviceModel.addFunction(functionModel);
                }
            }
        });
    }

    public static Optional<Function> getResourceFunctionModel() {
        InputStream resourceStream = Utils.class.getClassLoader()
                .getResourceAsStream("functions/http_resource.json");
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Function.class));
        } catch (IOException e) {
            return Optional.empty();
        }
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

    public static void updateFunctionInfo(Function functionModel, Function commonFunction) {
        functionModel.setEnabled(true);
        functionModel.setKind(commonFunction.getKind());
        functionModel.setCodedata(commonFunction.getCodedata());
        updateValue(functionModel.getAccessor(), commonFunction.getAccessor());
        updateValue(functionModel.getName(), commonFunction.getName());
        updateValue(functionModel.getReturnType(), commonFunction.getReturnType());
        List<Parameter> parameters = functionModel.getParameters();
        parameters.removeIf(parameter -> commonFunction.getParameters().stream()
                .anyMatch(newParameter -> newParameter.getName().getValue()
                        .equals(parameter.getName().getValue())));
        commonFunction.getParameters().forEach(functionModel::addParameter);
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
        Value requiredFunctions = service.getProperty(ServiceModelGeneratorConstants.PROPERTY_REQUIRED_FUNCTIONS);
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

    public static String getServiceDeclarationNode(Service service, FunctionAddContext context) {
        StringBuilder builder = new StringBuilder();
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
        if (isNewTcpService) {
            String serviceClassName = service.getProperties().get("returningServiceClass").getValue();
            String onConnectFunc = Utils.getTcpOnConnectTemplate().formatted(serviceClassName, serviceClassName);
            functions.add(onConnectFunc);
        } else {
            boolean isAiAgent = service.getModuleName().equals("ai.agent");
            FunctionBodyKind kind = isAiAgent ? FunctionBodyKind.EMPTY : FunctionBodyKind.DO_BLOCK;
            service.getFunctions().forEach(function -> {
                if (function.isEnabled()) {
                    String functionNode = "\t" + getFunction(function, new ArrayList<>(), kind, context)
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

    public static String getFunction(Function function, List<String> statusCodeResponses,
                                     FunctionBodyKind kind, FunctionAddContext context) {
        StringBuilder builder = new StringBuilder();
        String functionQualifiers = getFunctionQualifiers(function);
        if (!functionQualifiers.isEmpty()) {
            builder.append(functionQualifiers);
            builder.append(" ");
        }
        builder.append("function ");
        if (function.getKind().equals(ServiceModelGeneratorConstants.KIND_RESOURCE)
                && Objects.nonNull(function.getAccessor())
                && function.getAccessor().isEnabledWithValue()) {
            builder.append(getValueString(function.getAccessor()).toLowerCase(Locale.ROOT));
            builder.append(" ");
        }
        if (function.getKind().equals(ServiceModelGeneratorConstants.KIND_SUBSCRIPTION)) {
            builder.append(ServiceModelGeneratorConstants.SUBSCRIBE);
            builder.append(" ");
        }
        if (function.getKind().equals(ServiceModelGeneratorConstants.KIND_QUERY)) {
            builder.append(ServiceModelGeneratorConstants.GET);
            builder.append(" ");
        }
        builder.append(getValueString(function.getName()));
        builder.append(getFunctionSignature(function, statusCodeResponses));
        builder.append("{");
        builder.append(System.lineSeparator());
        if (kind.equals(FunctionBodyKind.DO_BLOCK) || kind.equals(FunctionBodyKind.BLOCK_WITH_PANIC)) {
            builder.append("\tdo {");
            builder.append(System.lineSeparator());
            if (context.equals(FunctionAddContext.HTTP_SERVICE_ADD)) {
                builder.append("\t\treturn \"Hello, Greetings!\";");
                builder.append(System.lineSeparator());
            }
        }
        if (kind.equals(FunctionBodyKind.BLOCK_WITH_PANIC)) {
            builder.append("\t\tpanic error(\"Unimplemented function\");");
            builder.append(System.lineSeparator());
            builder.append("\t} on fail error err {");
            builder.append(System.lineSeparator());
            builder.append("\t\t// handle error");
            builder.append(System.lineSeparator());
            builder.append("\t\tpanic error(\"Unhandled error\");");
            builder.append(System.lineSeparator());
            builder.append("\t}");
            builder.append(System.lineSeparator());
        }
        if (kind.equals(FunctionBodyKind.DO_BLOCK)) {
            builder.append("\t} on fail error err {");
            builder.append(System.lineSeparator());
            builder.append("\t\t// handle error");
            builder.append(System.lineSeparator());
            builder.append("\t\tpanic error(\"Unhandled error\");");
            builder.append(System.lineSeparator());
            builder.append("\t}");
            builder.append(System.lineSeparator());
        }
        builder.append("}");
        return builder.toString();
    }

    public enum FunctionBodyKind {
        EMPTY,
        BLOCK_WITH_PANIC,
        DO_BLOCK
    }

    public static String getFunctionSignature(Function function, List<String> statusCodeResponses) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        List<String> params = new ArrayList<>();
        function.getParameters().forEach(param -> {
            if (param.isEnabled()) {
                String paramDef;
                Value defaultValue = param.getDefaultValue();
                if (Objects.nonNull(defaultValue) && defaultValue.isEnabled()
                        && defaultValue.getValue() != null && !defaultValue.getValue().isEmpty()) {
                    paramDef = String.format("%s %s = %s", getValueString(param.getType()),
                            getValueString(param.getName()), getValueString(defaultValue));
                } else {
                    paramDef = String.format("%s %s", getValueString(param.getType()),
                            getValueString(param.getName()));
                }
                if (Objects.nonNull(param.getHttpParamType()) && !param.getHttpParamType().equals("Query")) {
                    paramDef = String.format("@http:%s %s", param.getHttpParamType(), paramDef);
                }
                params.add(paramDef);
            }
        });
        builder.append(String.join(", ", params));
        builder.append(")");
        FunctionReturnType returnType = function.getReturnType();
        if (Objects.nonNull(returnType)) {
            if (returnType.isEnabledWithValue()) {
                builder.append(" returns ");
                builder.append(getValueString(returnType));
            } else if (returnType.isEnabled() && Objects.nonNull(returnType.getResponses()) &&
                    !returnType.getResponses().isEmpty()) {
                List<String> responses = returnType.getResponses().stream()
                        .filter(HttpResponse::isEnabled)
                        .map(response -> HttpUtil.getStatusCodeResponse(response, statusCodeResponses))
                        .toList();
                if (!responses.isEmpty()) {
                    builder.append(" returns ");
                    builder.append(String.join("|", responses));
                }
            }
        }
        builder.append(" ");
        return builder.toString();
    }

    public static String getFunctionQualifiers(Function function) {
        List<String> qualifiers = function.getQualifiers();
        qualifiers = Objects.isNull(qualifiers) ? new ArrayList<>() : qualifiers;
        String kind = function.getKind();
        switch (kind) {
            case ServiceModelGeneratorConstants.KIND_QUERY, ServiceModelGeneratorConstants.KIND_SUBSCRIPTION,
                 ServiceModelGeneratorConstants.KIND_RESOURCE ->
                    qualifiers.add(ServiceModelGeneratorConstants.RESOURCE);
            case ServiceModelGeneratorConstants.KIND_REMOTE, ServiceModelGeneratorConstants.KIND_MUTATION ->
                    qualifiers.add(ServiceModelGeneratorConstants.REMOTE);

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

    /**
     * Generates the URI for the given source path.
     *
     * @param sourcePath the source path
     * @return the generated URI as a string
     */
    public static String getExprUri(String sourcePath) {
        String exprUriString = "expr" + Paths.get(sourcePath).toUri().toString().substring(4);
        return URI.create(exprUriString).toString();
    }

    public static boolean isTcpService(String org, String module) {
        return org.equals("ballerina") && module.equals("tcp");
    }

    public static String getTcpOnConnectTemplate() {
        return "    remote function onConnect(tcp:Caller caller) returns tcp:ConnectionService {%n" +
                "        do {%n" +
                "            %s connectionService = new %s();%n" +
                "            return connectionService;%n" +
                "        } on fail error err {%n" +
                "            // handle error%n" +
                "            panic error(\"Unhandled error\", err);%n" +
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

}
