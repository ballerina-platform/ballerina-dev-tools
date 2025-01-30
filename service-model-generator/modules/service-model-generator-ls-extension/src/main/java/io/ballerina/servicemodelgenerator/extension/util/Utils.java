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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.MethodDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NameReferenceNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ObjectTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.HttpResponse;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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

    public static final Map<String, String> HTTP_CODES;
    static {
        Map<String, String> httpCodeMap = new HashMap<>();
        httpCodeMap.put("Continue", "100");
        httpCodeMap.put("SwitchingProtocols", "101");
        httpCodeMap.put("Processing", "102");
        httpCodeMap.put("EarlyHints", "103");
        httpCodeMap.put("Ok", "200");
        httpCodeMap.put("Created", "201");
        httpCodeMap.put("Accepted", "202");
        httpCodeMap.put("NonAuthoritativeInformation", "203");
        httpCodeMap.put("NoContent", "204");
        httpCodeMap.put("ResetContent", "205");
        httpCodeMap.put("PartialContent", "206");
        httpCodeMap.put("MultiStatus", "207");
        httpCodeMap.put("AlreadyReported", "208");
        httpCodeMap.put("IMUsed", "226");
        httpCodeMap.put("MultipleChoices", "300");
        httpCodeMap.put("MovedPermanently", "301");
        httpCodeMap.put("Found", "302");
        httpCodeMap.put("SeeOther", "303");
        httpCodeMap.put("NotModified", "304");
        httpCodeMap.put("UseProxy", "305");
        httpCodeMap.put("TemporaryRedirect", "307");
        httpCodeMap.put("PermanentRedirect", "308");
        httpCodeMap.put("BadRequest", "400");
        httpCodeMap.put("Unauthorized", "401");
        httpCodeMap.put("PaymentRequired", "402");
        httpCodeMap.put("Forbidden", "403");
        httpCodeMap.put("NotFound", "404");
        httpCodeMap.put("MethodNotAllowed", "405");
        httpCodeMap.put("NotAcceptable", "406");
        httpCodeMap.put("ProxyAuthenticationRequired", "407");
        httpCodeMap.put("RequestTimeout", "408");
        httpCodeMap.put("Conflict", "409");
        httpCodeMap.put("Gone", "410");
        httpCodeMap.put("LengthRequired", "411");
        httpCodeMap.put("PreconditionFailed", "412");
        httpCodeMap.put("PayloadTooLarge", "413");
        httpCodeMap.put("UriTooLong", "414");
        httpCodeMap.put("UnsupportedMediaType", "415");
        httpCodeMap.put("RangeNotSatisfiable", "416");
        httpCodeMap.put("ExpectationFailed", "417");
        httpCodeMap.put("MisdirectedRequest", "421");
        httpCodeMap.put("UnprocessableEntity", "422");
        httpCodeMap.put("Locked", "423");
        httpCodeMap.put("FailedDependency", "424");
        httpCodeMap.put("TooEarly", "425");
        httpCodeMap.put("UpgradeRequired", "426");
        httpCodeMap.put("PreconditionRequired", "428");
        httpCodeMap.put("TooManyRequests", "429");
        httpCodeMap.put("RequestHeaderFieldsTooLarge", "431");
        httpCodeMap.put("UnavailableDueToLegalReasons", "451");
        httpCodeMap.put("InternalServerError", "500");
        httpCodeMap.put("NotImplemented", "501");
        httpCodeMap.put("BadGateway", "502");
        httpCodeMap.put("ServiceUnavailable", "503");
        httpCodeMap.put("GatewayTimeout", "504");
        httpCodeMap.put("HttpVersionNotSupported", "505");
        httpCodeMap.put("VariantAlsoNegotiates", "506");
        httpCodeMap.put("InsufficientStorage", "507");
        httpCodeMap.put("LoopDetected", "508");
        httpCodeMap.put("NotExtended", "510");
        httpCodeMap.put("NetworkAuthenticationRequired", "511");
        HTTP_CODES = Collections.unmodifiableMap(httpCodeMap);
    }

    public static final Map<String, String> HTTP_CODES_DES;
    static {
        Map<String, String> httpCodeMap = new HashMap<>();
        httpCodeMap.put("100", "Continue");
        httpCodeMap.put("101", "SwitchingProtocols");
        httpCodeMap.put("102", "Processing");
        httpCodeMap.put("103", "EarlyHints");
        httpCodeMap.put("200", "Ok");
        httpCodeMap.put("201", "Created");
        httpCodeMap.put("202", "Accepted");
        httpCodeMap.put("203", "NonAuthoritativeInformation");
        httpCodeMap.put("204", "NoContent");
        httpCodeMap.put("205", "ResetContent");
        httpCodeMap.put("206", "PartialContent");
        httpCodeMap.put("207", "MultiStatus");
        httpCodeMap.put("208", "AlreadyReported");
        httpCodeMap.put("226", "IMUsed");
        httpCodeMap.put("300", "MultipleChoices");
        httpCodeMap.put("301", "MovedPermanently");
        httpCodeMap.put("302", "Found");
        httpCodeMap.put("303", "SeeOther");
        httpCodeMap.put("304", "NotModified");
        httpCodeMap.put("305", "UseProxy");
        httpCodeMap.put("307", "TemporaryRedirect");
        httpCodeMap.put("308", "PermanentRedirect");
        httpCodeMap.put("400", "BadRequest");
        httpCodeMap.put("401", "Unauthorized");
        httpCodeMap.put("402", "PaymentRequired");
        httpCodeMap.put("403", "Forbidden");
        httpCodeMap.put("404", "NotFound");
        httpCodeMap.put("405", "MethodNotAllowed");
        httpCodeMap.put("406", "NotAcceptable");
        httpCodeMap.put("407", "ProxyAuthenticationRequired");
        httpCodeMap.put("408", "RequestTimeOut");
        httpCodeMap.put("409", "Conflict");
        httpCodeMap.put("410", "Gone");
        httpCodeMap.put("411", "LengthRequired");
        httpCodeMap.put("412", "PreconditionFailed");
        httpCodeMap.put("413", "PayloadTooLarge");
        httpCodeMap.put("414", "UriTooLong");
        httpCodeMap.put("415", "UnsupportedMediaType");
        httpCodeMap.put("416", "RangeNotSatisfiable");
        httpCodeMap.put("417", "ExpectationFailed");
        httpCodeMap.put("421", "MisdirectedRequest");
        httpCodeMap.put("422", "UnprocessableEntity");
        httpCodeMap.put("423", "Locked");
        httpCodeMap.put("424", "FailedDependency");
        httpCodeMap.put("425", "TooEarly");
        httpCodeMap.put("426", "UpgradeRequired");
        httpCodeMap.put("428", "PreconditionRequired");
        httpCodeMap.put("429", "TooManyRequests");
        httpCodeMap.put("431", "RequestHeaderFieldsTooLarge");
        httpCodeMap.put("451", "UnavailableDueToLegalReasons");
        httpCodeMap.put("500", "InternalServerError");
        httpCodeMap.put("501", "NotImplemented");
        httpCodeMap.put("502", "BadGateway");
        httpCodeMap.put("503", "ServiceUnavailable");
        httpCodeMap.put("504", "GatewayTimeout");
        httpCodeMap.put("505", "HttpVersionNotSupported");
        httpCodeMap.put("506", "VariantAlsoNegotiates");
        httpCodeMap.put("507", "InsufficientStorage");
        httpCodeMap.put("508", "LoopDetected");
        httpCodeMap.put("510", "NotExtended");
        httpCodeMap.put("511", "NetworkAuthenticationRequired");
        HTTP_CODES_DES = Collections.unmodifiableMap(httpCodeMap);
    }

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
        populateRequiredFunctions(service);
        populateServiceType(service);
        populateDesignApproach(service);
    }

    private static void populateRequiredFunctions(Service service) {
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

    public static Service getServiceModel(TypeDefinitionNode serviceTypeNode, SemanticModel semanticModel,
                                          boolean isHttp) {
        Service serviceModel = Service.getNewService();
        ObjectTypeDescriptorNode serviceNode = (ObjectTypeDescriptorNode) serviceTypeNode.typeDescriptor();
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
        serviceNode.members().forEach(member -> {
            if (member instanceof MethodDeclarationNode functionDefinitionNode) {
                Function functionModel = getFunctionModel(functionDefinitionNode, semanticModel, isHttp);
                functionModels.add(functionModel);
            }
        });
        serviceModel.setFunctions(functionModels);
        return serviceModel;
    }

    public static Service getServiceModel(ServiceDeclarationNode serviceDeclarationNode, SemanticModel semanticModel,
                                          boolean isHttp, boolean isGraphQL) {
        Service serviceModel = Service.getNewService();
        String basePath = getPath(serviceDeclarationNode.absoluteResourcePath());
        if (!basePath.isEmpty()) {
            Value basePathValue = new Value();
            basePathValue.setValue(basePath);
            basePathValue.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
            basePathValue.setEnabled(true);
            serviceModel.setBasePath(basePathValue);
        }
        Optional<TypeDescriptorNode> serviceTypeDesc = serviceDeclarationNode.typeDescriptor();
        if (serviceTypeDesc.isPresent()) {
            Value serviceType = new Value();
            serviceType.setValue(serviceTypeDesc.get().toString().trim());
            serviceType.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_TYPE);
            serviceType.setEnabled(true);
            if (isHttpServiceContractType(semanticModel, serviceTypeDesc.get())) {
                serviceModel.setServiceContractTypeName(serviceType);
            } else {
                serviceModel.setServiceType(serviceType);
            }
        }
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
                                            boolean isHttp) {
        Function functionModel = Function.getNewFunction();
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
            Optional<Parameter> parameterModel = getParameterModel(parameterNode);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        return functionModel;
    }

    public static Function getFunctionModel(FunctionDefinitionNode functionDefinitionNode,
                                            SemanticModel semanticModel, boolean isHttp, boolean isGraphQL) {
        Function functionModel = Function.getNewFunction();
        functionModel.setEnabled(true);
        Value accessor = functionModel.getAccessor();
        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.functionName().text().trim());
        functionName.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER);
        functionName.setEnabled(true);
        if (isGraphQL) {
            accessor.setEditable(false);
            functionModel.setSchema(Map.of(ServiceModelGeneratorConstants.PARAMETER, Parameter.parameterSchema()));
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
            Optional<Parameter> parameterModel = getParameterModel(parameterNode);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        return functionModel;
    }

    private static void populateHttpResponses(MethodDeclarationNode functionDefinitionNode,
                                              FunctionReturnType returnType, SemanticModel semanticModel) {
        Optional<Symbol> functionDefSymbol = semanticModel.symbol(functionDefinitionNode);
        if (functionDefSymbol.isEmpty() || !(functionDefSymbol.get() instanceof ResourceMethodSymbol resource)) {
            return;
        }
        populateHttpResponses(returnType, semanticModel, resource);
    }

    private static void populateHttpResponses(FunctionDefinitionNode functionDefinitionNode,
                                              FunctionReturnType returnType, SemanticModel semanticModel) {
        Optional<Symbol> functionDefSymbol = semanticModel.symbol(functionDefinitionNode);
        if (functionDefSymbol.isEmpty() || !(functionDefSymbol.get() instanceof ResourceMethodSymbol resource)) {
            return;
        }
        populateHttpResponses(returnType, semanticModel, resource);
    }

    private static void populateHttpResponses(FunctionReturnType returnType, SemanticModel semanticModel,
                                              ResourceMethodSymbol resource) {
        Optional<TypeSymbol> returnTypeSymbol = resource.typeDescriptor().returnTypeDescriptor();
        if (returnTypeSymbol.isEmpty()) {
            return;
        }
        Optional<ModuleSymbol> module = resource.getModule();
        String currentModuleName = "";
        if (module.isPresent()) {
            currentModuleName = module.get().getName().orElse("");
        }
        Optional<String> method = resource.getName();
        if (method.isEmpty()) {
            return;
        }
        int defaultStatusCode = method.get().trim().equalsIgnoreCase("post") ? 201 : 200;
        List<HttpResponse> httpResponses = getHttpResponses(returnTypeSymbol.get(), defaultStatusCode, semanticModel,
                currentModuleName);
        returnType.setResponses(httpResponses);
    }

    private static List<HttpResponse> getHttpResponses(TypeSymbol returnTypeSymbol, int defaultStatusCode,
                                                       SemanticModel semanticModel, String currentModuleName) {
        List<TypeSymbol> statusCodeResponses = new ArrayList<>();
        List<TypeSymbol> anydataResponses = new ArrayList<>();
        Optional<UnionTypeSymbol> unionType = getUnionType(returnTypeSymbol);
        unionType.ifPresentOrElse(
                unionTypeSymbol -> unionTypeSymbol.memberTypeDescriptors().forEach(member -> {
                    if (isSubTypeOfHttpStatusCodeResponse(member, semanticModel)) {
                        statusCodeResponses.add(member);
                    } else {
                        anydataResponses.add(member);
                    }
                }),
                () -> {
                    if (isSubTypeOfHttpStatusCodeResponse(returnTypeSymbol, semanticModel)) {
                        statusCodeResponses.add(returnTypeSymbol);
                    } else {
                        anydataResponses.add(returnTypeSymbol);
                    }
                });
        List<HttpResponse> responses = new ArrayList<>(statusCodeResponses.stream()
                .map(statusCodeResponse -> getHttpResponse(statusCodeResponse, String.valueOf(defaultStatusCode),
                        semanticModel, currentModuleName))
                .toList());
        String normalResponseBody = anydataResponses.stream()
                .map(type -> getTypeName(type, currentModuleName))
                .collect(Collectors.joining("|"));
        if (!normalResponseBody.isEmpty()) {
            HttpResponse normalResponse = new HttpResponse(String.valueOf(defaultStatusCode), normalResponseBody,
                    normalResponseBody, normalResponseBody);
            responses.add(normalResponse);
        }
        return responses;
    }

    public static boolean isSubTypeOfHttpStatusCodeResponse(TypeSymbol typeSymbol, SemanticModel semanticModel) {
        return isSubTypeOfBallerinaModuleType("StatusCodeResponse", "http", typeSymbol, semanticModel);
    }

    static boolean isSubTypeOfBallerinaModuleType(String type, String moduleName, TypeSymbol typeSymbol,
                                                  SemanticModel semanticModel) {
        Optional<Symbol> optionalRecordSymbol = semanticModel.types().getTypeByName("ballerina", moduleName,
                "", type);
        if (optionalRecordSymbol.isPresent() &&
                optionalRecordSymbol.get() instanceof TypeDefinitionSymbol recordSymbol) {
            return typeSymbol.subtypeOf(recordSymbol.typeDescriptor());
        }
        return false;
    }

    private static String getResponseCode(TypeSymbol typeSymbol, String defaultCode, SemanticModel semanticModel) {
        for (Map.Entry<String, String> entry : HTTP_CODES.entrySet()) {
            if (isSubTypeOfBallerinaModuleType(entry.getKey(), "http", typeSymbol, semanticModel)) {
                return entry.getValue();
            }
        }
        if (isSubTypeOfBallerinaModuleType("DefaultStatusCodeResponse", "http", typeSymbol,
                semanticModel)) {
            return "default";
        }
        return defaultCode;
    }

    public static HttpResponse getHttpResponse(TypeSymbol statusCodeResponseType, String defaultStatusCode,
                                               SemanticModel semanticModel, String currentModuleName) {
        Optional<RecordTypeSymbol> statusCodeRecordType = getRecordTypeSymbol(statusCodeResponseType);
        String statusCode = getResponseCode(statusCodeResponseType, defaultStatusCode, semanticModel);
        TypeSymbol bodyType = semanticModel.types().ANYDATA;
        String name = null;
        if (statusCodeRecordType.isPresent()) {
            bodyType = getBodyType(statusCodeRecordType.get(), semanticModel);
            name = getTypeName(statusCodeResponseType, currentModuleName);
        }
        if (Objects.isNull(name)) {
            return new HttpResponse(statusCode, getTypeName(bodyType, currentModuleName));
        }
        return new HttpResponse(statusCode, getTypeName(bodyType, currentModuleName), name, name);
    }

    static String getTypeName(TypeSymbol typeSymbol, String currentModuleName) {
        String signature = typeSymbol.signature().trim();
        String[] parts = signature.split("[:/]");
        if (parts.length == 4) {
            return parts[1].equals(currentModuleName) ? parts[3] : parts[1] + ":" + parts[3];
        }
        return signature;
    }

    static TypeSymbol getBodyType(RecordTypeSymbol responseRecordType, SemanticModel semanticModel) {
        if (Objects.nonNull(responseRecordType) && responseRecordType.fieldDescriptors().containsKey("body")) {
            return responseRecordType.fieldDescriptors().get("body").typeDescriptor();
        }
        return semanticModel.types().ANYDATA;
    }

    static Optional<RecordTypeSymbol> getRecordTypeSymbol(TypeSymbol typeSymbol) {
        TypeSymbol statusCodeResType = getReferredType(typeSymbol);
        if (statusCodeResType instanceof TypeReferenceTypeSymbol statusCodeResRefType &&
                statusCodeResRefType.typeDescriptor() instanceof RecordTypeSymbol recordTypeSymbol) {
            return Optional.of(recordTypeSymbol);
        } else if (statusCodeResType instanceof RecordTypeSymbol recordTypeSymbol) {
            return Optional.of(recordTypeSymbol);
        }
        return Optional.empty();
    }

    public static TypeSymbol getReferredType(TypeSymbol typeSymbol) {
        if (typeSymbol.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
            TypeSymbol referencedType = ((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor();
            if (referencedType.typeKind().equals(TypeDescKind.TYPE_REFERENCE)) {
                return getReferredType(referencedType);
            } else {
                return typeSymbol;
            }
        }
        return typeSymbol;
    }

    private static Optional<UnionTypeSymbol> getUnionType(TypeSymbol typeSymbol) {
        if (Objects.isNull(typeSymbol)) {
            return Optional.empty();
        }
        return switch (typeSymbol.typeKind()) {
            case UNION -> Optional.of((UnionTypeSymbol) typeSymbol);
            case TYPE_REFERENCE -> getUnionType(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
            default -> Optional.empty();
        };
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

    public static Optional<Parameter> getParameterModel(ParameterNode parameterNode) {
        if (parameterNode instanceof RequiredParameterNode parameter) {
            String paramName = parameter.paramName().get().toString().trim();
            Parameter parameterModel = createParameter(paramName, ServiceModelGeneratorConstants.VALUE_TYPE_IDENTIFIER,
                    parameter.typeName().toString().trim(), parameter.annotations());
            return Optional.of(parameterModel);
        } else if (parameterNode instanceof DefaultableParameterNode parameter) {
            String paramName = parameter.paramName().get().toString().trim();
            Parameter parameterModel = createParameter(paramName, ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION,
                    parameter.typeName().toString().trim(), parameter.annotations());
            Value defaultValue = parameterModel.getDefaultValue();
            defaultValue.setValue(parameter.expression().toString().trim());
            defaultValue.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION);
            defaultValue.setEnabled(true);
            return Optional.of(parameterModel);
        }
        return Optional.empty();
    }

    private static Parameter createParameter(String paramName, String valueType, String typeName,
                                             NodeList<AnnotationNode> annotationNodes) {
        Parameter parameterModel = Parameter.getNewParameter();
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

    public static void updateListenerModel(Listener listener, ListenerDeclarationNode listenerNode) {
        Optional<Listener> commonListener = getListenerModel(listenerNode);
        commonListener.ifPresent(listenerModel -> {
                listenerModel.getProperties().forEach((key, value) ->
                        updateValue(listener.getProperty(key), value));
                listener.setCodedata(new Codedata(listenerNode.lineRange()));
        });
    }

    public static void updateServiceContractModel(Service serviceModel, TypeDefinitionNode serviceTypeNode,
                                                  ServiceDeclarationNode serviceDeclaration,
                                                  SemanticModel semanticModel) {
        serviceModel.setFunctions(new ArrayList<>());
        Service commonSvcModel = getServiceModel(serviceTypeNode, semanticModel, true);
        updateServiceInfo(serviceModel, commonSvcModel);
        serviceModel.setCodedata(new Codedata(serviceDeclaration.lineRange()));
        populateListenerInfo(serviceModel, serviceDeclaration);
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

    public static void updateServiceModel(Service serviceModel, ServiceDeclarationNode serviceNode,
                                          SemanticModel semanticModel) {
        String moduleName = serviceModel.getModuleName();
        boolean isHttp = moduleName.equals(ServiceModelGeneratorConstants.HTTP);
        boolean isGraphql = moduleName.equals(ServiceModelGeneratorConstants.GRAPHQL);
        if (isHttp || isGraphql) {
            serviceModel.setFunctions(new ArrayList<>());
        }
        Service commonSvcModel = getServiceModel(serviceNode, semanticModel, isHttp, isGraphql);
        updateServiceInfo(serviceModel, commonSvcModel);
        serviceModel.setCodedata(new Codedata(serviceNode.lineRange()));
        populateListenerInfo(serviceModel, serviceNode);
    }

    private static void updateServiceInfo(Service serviceModel, Service commonSvcModel) {
        Value serviceContractTypeNameValue = commonSvcModel.getServiceContractTypeNameValue();
        if (Objects.nonNull(serviceContractTypeNameValue)) {
            enableContractFirstApproach(serviceModel);
        }
        populateDesignApproach(serviceModel);
        if (Objects.nonNull(serviceModel.getServiceType()) && Objects.nonNull(commonSvcModel.getServiceType())) {
            serviceModel.updateServiceType(commonSvcModel.getServiceType());
        }
        populateProperties(serviceModel);
        if (Objects.nonNull(commonSvcModel.getBasePath())) {
            if (Objects.nonNull(commonSvcModel.getBasePath())) {
                updateValue(serviceModel.getBasePath(), commonSvcModel.getBasePath());
            } else {
                serviceModel.setBasePath(commonSvcModel.getBasePath());
            }
        }
        updateValue(serviceModel.getServiceContractTypeNameValue(), commonSvcModel.getServiceContractTypeNameValue());
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
                    switch (functionModel.getKind()) {
                        case ServiceModelGeneratorConstants.KIND_QUERY ->
                                functionModel.setMetadata(graphqlQueryMetaData());
                        case ServiceModelGeneratorConstants.KIND_MUTATION ->
                                functionModel.setMetadata(graphqlMutationMetaData());
                        case ServiceModelGeneratorConstants.KIND_SUBSCRIPTION ->
                                functionModel.setMetadata(graphqlSubscriptionMetaData());
                        default -> { }
                    }
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

    private static void updateFunctionInfo(Function functionModel, Function commonFunction) {
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

    private static void populateListenerInfo(Service serviceModel, ServiceDeclarationNode serviceNode) {
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
        NodeList<Node> paths = serviceNode.absoluteResourcePath();
        if (!paths.isEmpty()) {
            serviceModel.getBasePath().setValue(getPath(paths));
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

    private static boolean isPresent(Function functionModel, Function newFunction) {
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

    public static String getServiceDeclarationNode(Service service) {
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
        }
        builder.append(ServiceModelGeneratorConstants.ON).append(ServiceModelGeneratorConstants.SPACE);
        if (Objects.nonNull(service.getListener()) && service.getListener().isEnabledWithValue()) {
            builder.append(service.getListener().getValue());
        }
        builder.append(ServiceModelGeneratorConstants.SPACE).append(ServiceModelGeneratorConstants.OPEN_BRACE);
        builder.append(System.lineSeparator());
        List<String> functions = new ArrayList<>();
        service.getFunctions().forEach(function -> {
            if (function.isEnabled()) {
                String functionNode = "\t" + getFunction(function, new ArrayList<>()).replace(System.lineSeparator(),
                        System.lineSeparator() + "\t");
                functions.add(functionNode);
            }
        });
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
            if (val.isEnabledWithValue()) {
                params.add(String.format("%s: %s", key, getValueString(val)));
            }
        });
        return String.format("{%s}", String.join(", ", params));
    }

    public static String getFunction(Function function, List<String> statusCodeResponses) {
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

    public static String getFunctionSignature(Function function, List<String> statusCodeResponses) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        List<String> params = new ArrayList<>();
        function.getParameters().forEach(param -> {
            if (param.isEnabled()) {
                String paramDef;
                if (Objects.nonNull(param.getDefaultValue()) && param.getDefaultValue().isEnabled()) {
                    paramDef = String.format("%s %s = %s", getValueString(param.getType()),
                            getValueString(param.getName()), getValueString(param.getDefaultValue()));
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
                builder.append(" returns ");
                List<String> responses = returnType.getResponses().stream()
                        .map(response -> getStatusCodeResponse(response, statusCodeResponses))
                        .toList();
                builder.append(String.join("|", responses));
            }
        }
        builder.append(" ");
        return builder.toString();
    }

    public static String getStatusCodeResponse(HttpResponse response, List<String> statusCodeResponses) {
        if (Objects.nonNull(response.getType()) && response.getType().isEnabledWithValue()) {
            return response.getType().getValue();
        }
        if (Objects.isNull(response.getBody()) || !response.getBody().isEnabledWithValue()) {
            if (!response.getStatusCode().isEnabledWithValue()) {
                return "anydata";
            }
            String statusCode = response.getStatusCode().getValue();
            String statusCodeRes = HTTP_CODES_DES.get(statusCode);
            if (Objects.isNull(statusCodeRes)) {
                return "anydata";
            }
            return String.format("http:%s", statusCodeRes);
        }
        String body = response.getBody().getValue();
        String statusCode = response.getStatusCode().getValue();
        String statusCodeRes = HTTP_CODES_DES.get(statusCode);
        if (Objects.isNull(statusCodeRes)) {
            return body;
        }
        if (Objects.nonNull(response.isCreateStatusCodeResponse()) &&
                response.isCreateStatusCodeResponse().isEnabledWithValue() &&
                response.getName().isEnabledWithValue()) {
            statusCodeResponses.add(getStatusCodeResponseDef(statusCodeRes, body, response.getName().getValue()));
            return response.getName().getValue();
        }
        return String.format("record {|*http:%s; %s body;|}", statusCodeRes, body);
    }

    public static String getStatusCodeResponseDef(String statusCodeTypeName, String body, String name) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("public type %s record {|", name));
        builder.append(System.lineSeparator());
        builder.append(String.format("\t*http:%s;", statusCodeTypeName));
        builder.append(System.lineSeparator());
        builder.append(String.format("\t%s body;", body));
        builder.append(System.lineSeparator());
        builder.append("|};");
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
        newExpressionNode.parenthesizedArgList().ifPresent(argList ->
                argList.arguments().forEach(arg -> {
                    if (arg instanceof NamedArgumentNode namedArgumentNode) {
                        Value value = new Value();
                        value.setValue(namedArgumentNode.expression().toString().trim());
                        value.setEnabled(true);
                        value.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION);
                        properties.put(namedArgumentNode.argumentName().name().text().trim(), value);
                    }
                })
        );
        Value nameValue = new Value();
        nameValue.setEnabled(true);
        nameValue.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION);
        nameValue.setValue(listenerDeclarationNode.variableName().text().trim());
        properties.put(ServiceModelGeneratorConstants.PROPERTY_NAME, nameValue);
        Codedata codedata = new Codedata(listenerDeclarationNode.lineRange());
        return Optional.of(new Listener(null, null, null, null, null, null, null, null, null, null, listenerProtocol,
                null, properties, codedata));
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

    private static MetaData graphqlSubscriptionMetaData() {
        return new MetaData("Graphql Subscription", "Graphql Subscription");
    }

    private static MetaData graphqlQueryMetaData() {
        return new MetaData("Graphql Query", "Graphql Query");
    }

    private static MetaData graphqlMutationMetaData() {
        return new MetaData("Graphql Mutation", "Graphql Mutation");
    }
}
