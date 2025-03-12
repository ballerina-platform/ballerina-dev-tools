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
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.HttpResponse;
import io.ballerina.servicemodelgenerator.extension.model.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for HTTP related operations.
 *
 * @since 2.0.0
 */
public final class HttpUtil {

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

    private HttpUtil() {
    }

    public static void populateHttpResponses(FunctionReturnType returnType, SemanticModel semanticModel,
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
        AtomicBoolean hasHttpResponse = new AtomicBoolean(false);
        unionType.ifPresentOrElse(
                unionTypeSymbol -> unionTypeSymbol.memberTypeDescriptors().forEach(member -> {
                    if (isSubTypeOfHttpStatusCodeResponse(member, semanticModel)) {
                        statusCodeResponses.add(member);
                    } else if (isHttpResponse(getTypeName(member, currentModuleName))) {
                        hasHttpResponse.set(true);
                    } else {
                        anydataResponses.add(member);
                    }
                }),
                () -> {
                    if (isSubTypeOfHttpStatusCodeResponse(returnTypeSymbol, semanticModel)) {
                        statusCodeResponses.add(returnTypeSymbol);
                    } else if (isHttpResponse(getTypeName(returnTypeSymbol, currentModuleName))) {
                        hasHttpResponse.set(true);
                    } else {
                        anydataResponses.add(returnTypeSymbol);
                    }
                });
        List<HttpResponse> responses = new ArrayList<>();
        HttpResponse normalResponse = new HttpResponse(String.valueOf(defaultStatusCode), "http:Response");
        normalResponse.setAdvanced(true);
        normalResponse.setEditable(true);
        normalResponse.setEnabled(hasHttpResponse.get());
        normalResponse.setHttpResponseType(true);

        responses.add(normalResponse);

        statusCodeResponses.stream()
                .map(statusCodeResponse -> getHttpResponse(statusCodeResponse, String.valueOf(defaultStatusCode),
                        semanticModel, currentModuleName))
                .forEach(responses::add);

        anydataResponses.stream()
                .map(type -> getTypeName(type, currentModuleName))
                .forEach(type -> {
                    HttpResponse response = new HttpResponse(String.valueOf(defaultStatusCode), type);
                    response.setEnabled(true);
                    response.setEditable(true);
                    responses.add(response);
                });

        return responses;
    }

    private static boolean isHttpResponse(String type) {
        return type.trim().equals("http:Response");
    }

    private static HttpResponse getHttpResponse(TypeSymbol statusCodeResponseType, String defaultStatusCode,
                                               SemanticModel semanticModel, String currentModuleName) {
        String typeName = getTypeName(statusCodeResponseType, currentModuleName);
        String statusCode = getResponseCode(statusCodeResponseType, defaultStatusCode, semanticModel);
        if (typeName.contains("}")) {
            return HttpResponse.getAnonResponse(statusCode, "record {|...|}");
        }
        boolean addEditButton = typeName.startsWith(currentModuleName + ":");
        return new HttpResponse(statusCode, typeName, addEditButton);
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

    public static String getStatusCodeResponse(HttpResponse response, List<String> statusCodeResponses) {
        Value name = response.getName();
        if (Objects.nonNull(name) && name.isEnabledWithValue()) {
            String statusCode = response.getStatusCode().getValue();
            String statusCodeRes = HTTP_CODES_DES.get(statusCode);
            if (Objects.isNull(statusCodeRes)) {
                return response.getName().getValue();
            }
            statusCodeResponses.add(getNewResponseTypeStr(statusCodeRes, name.getValue(), response.getBody(),
                    response.getHeaders()));
            return response.getName().getValue();
        }
        if (response.getType().isEnabledWithValue()) {
            return response.getType().getValue();
        }
        if (Objects.nonNull(response.getBody()) && response.getBody().isEnabledWithValue()) {
            return response.getBody().getValue();
        }
        Value statusCode = response.getStatusCode();
        if (Objects.nonNull(statusCode) && statusCode.isEnabledWithValue()) {
            String statusCodeRes = HTTP_CODES_DES.get(statusCode.getValue().trim());
            if (Objects.nonNull(statusCodeRes)) {
                return "http:" + statusCodeRes;
            }
        }
        return null;
    }

    private static String getNewResponseTypeStr(String statusCodeTypeName, String name, Value body, Value headers) {
        String template = "public type %s record {|%n\t*http:%s;".formatted(name, statusCodeTypeName);
        if (Objects.nonNull(body) && body.isEnabledWithValue()) {
            template += "\t%s body;%n".formatted(body.getValue());
        }
        if (Objects.nonNull(headers) && headers.isEnabledWithValue()) {
            template += "\tmap<%s> headers;%n".formatted(String.join("|", headers.getValues()));
        }
        template += "|};";
        return template;
    }
}
