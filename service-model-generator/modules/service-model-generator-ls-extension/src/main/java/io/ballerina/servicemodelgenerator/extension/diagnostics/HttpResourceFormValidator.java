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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.EnumSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ConstantDeclarationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.HttpResponse;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.servicemodelgenerator.extension.diagnostics.ReservedKeywords.KEYWORD_LIST;

public class HttpResourceFormValidator {

    private final Context ctx;
    private final SemanticModel semanticModel;
    private final Document document;
    private TypeSymbol basicType;
    private TypeSymbol queryTypeConstrain;
    private TypeSymbol headerBasicType;
    private List<ConstantSymbol> constantSymbols;
    private List<EnumSymbol> enumSymbols;
    private TypeSymbol errorOrNilType;
    private boolean isConstAndEnumsLoaded = false;
    private final IdentifierValidator identifierValidator = new IdentifierValidator();

    public HttpResourceFormValidator(Context ctx, SemanticModel semanticModel, Document document) {
        this.ctx = ctx;
        this.semanticModel = semanticModel;
        this.document = document;
        initBasicTypes();
    }

    public enum Context {
        ADD,
        UPDATE
    }

    private void initBasicTypes() {
        Types types = semanticModel.types();
        this.basicType =  types.builder().UNION_TYPE.withMemberTypes(types.BOOLEAN, types.INT,
                types.FLOAT, types.DECIMAL, types.STRING).build();

        TypeSymbol queryBasicType = types.builder().UNION_TYPE.withMemberTypes(basicType,
                types.builder().MAP_TYPE.withTypeParam(types.ANYDATA).build()).build();
        this.queryTypeConstrain = types.builder().UNION_TYPE.withMemberTypes(types.NIL, queryBasicType,
                types.builder().ARRAY_TYPE.withType(queryBasicType).build()).build();

        this.headerBasicType = types.builder().UNION_TYPE.withMemberTypes(types.NIL, basicType,
                types.builder().ARRAY_TYPE.withType(basicType).build(),
                types.builder().RECORD_TYPE.withRestField(basicType).build()).build();

        this.errorOrNilType = types.builder().UNION_TYPE.withMemberTypes(types.NIL, types.ERROR).build();
    }

    private void loadConstAndEnums() {
        if (isConstAndEnumsLoaded) {
            return;
        }
        constantSymbols = semanticModel.moduleSymbols().stream()
                .filter(symbol -> symbol instanceof ConstantSymbol)
                .map(symbol -> ((ConstantSymbol) symbol))
                .toList();
        enumSymbols = semanticModel.moduleSymbols().stream()
                .filter(symbol -> symbol instanceof EnumSymbol)
                .map(symbol -> ((EnumSymbol) symbol))
                .toList();
        isConstAndEnumsLoaded = true;
    }

    public void validate(Function function, ServiceDeclarationNode serviceDeclarationNode) {
        validate(function, serviceDeclarationNode, null, null);
    }

    public void validate(Function function, ServiceDeclarationNode serviceDeclarationNode,
                         FunctionDefinitionNode functionDefinitionNode, Codedata codedata) {
        String accessor = function.getAccessor().getValue().toLowerCase(Locale.ROOT);

        String resourceName = function.getName().getValue();
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        if (!validateResourcePath(resourceName, diagnostics, paramNames)) {
            function.getName().setDiagnostics(new Diagnostics(true, diagnostics));
            return;
        }

        if (!uniqueResourcePath(serviceDeclarationNode, resourceName, accessor, diagnostics, functionDefinitionNode,
                codedata)) {
            function.getName().setDiagnostics(new Diagnostics(true, diagnostics));
            return;
        }

        // load the document and see
        function.getName().setDiagnostics(null);

        List<Parameter> parameters = function.getParameters();
        boolean hasHttpCaller = false;
        for (Parameter param: parameters) {
            if (param.isEnabled()) {
                Value paramType = param.getType();
                Value paramName = param.getName();
                if (paramType.getValue().equals("http:Caller")) {
                    hasHttpCaller = true;
                }

                if (!validIdentifier(identifierValidator, paramName.getValue().trim(), diagnostics)) {
                    return;
                }

                if (!paramNames.add(paramName.getValue())) {
                    paramName.setDiagnostics(new Diagnostics(true, List.of(
                            new Diagnostics.Info(DiagnosticSeverity.ERROR, "duplicate parameter name: '" +
                                    paramName.getValue() + "'")
                    )));
                    return;
                }

                String httpParamType = param.getHttpParamType();
                if ("Query".equals(httpParamType)) {
                    if (!validQueryOrHeaderType(paramType.getValue().trim(), queryTypeConstrain)) {
                        paramType.setDiagnostics(new Diagnostics(true, List.of(
                                new Diagnostics.Info(DiagnosticSeverity.ERROR, "invalid query param type")
                        )));
                        return;
                    }
                    return;
                } else if ("Header".equals(httpParamType)) {
                    if (!validQueryOrHeaderType(paramType.getValue().trim(), headerBasicType)) {
                        paramType.setDiagnostics(new Diagnostics(true, List.of(
                                new Diagnostics.Info(DiagnosticSeverity.ERROR, "invalid header param type")
                        )));
                        return;
                    }
                }
                paramType.setDiagnostics(null);
                paramName.setDiagnostics(null);
            }
        }

        FunctionReturnType returnType = function.getReturnType();
        if (Objects.nonNull(returnType)) {
            List<HttpResponse> httpResponses = returnType.getResponses();
            if (Objects.isNull(httpResponses)) {
                return;
            }
            for (HttpResponse httpResponse: httpResponses) {
                httpResponse.clearDiagnostics();
                if (!httpResponse.isEnabled()) {
                    continue;
                }
                if (!validHttpResponse(httpResponse, hasHttpCaller)) {
                    return;
                }
            }
        }
    }

    private boolean uniqueResourcePath(ServiceDeclarationNode node, String resourceName, String accessor,
                                       List<Diagnostics.Info> diagnostics, FunctionDefinitionNode updatingFunc,
                                       Codedata codedata) {
        boolean isUpdateFunction = ctx == Context.UPDATE;

        for (Node member: node.members()) {
            if (!(member instanceof FunctionDefinitionNode functionDefinitionNode)) {
                continue;
            }
            if (!functionDefinitionNode.qualifierList().stream().map(Token::text).toList().contains(
                    Qualifier.RESOURCE.getValue())) {
                continue;
            }

            if (isUpdateFunction && updatingFunc.lineRange().startLine().line() ==
                    codedata.getLineRange().startLine().line()) {
                continue;
            }

            // check if another function exist with the same accessor and function name
            if (!(Utils.getPath(functionDefinitionNode.relativeResourcePath()).equals(resourceName) &&
                    functionDefinitionNode.functionName().text().trim().equals(accessor))) {
                continue;
            }
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, "resource path is already defined"));
            return false;
        }
        return true;
    }

    private boolean validHttpResponse(HttpResponse response, boolean hasHttpCaller) {
        Value name = response.getName();
        if (Objects.nonNull(name) && name.isEnabledWithValue()) {
            if (hasHttpCaller) {
                response.setDiagnostics(new Diagnostics(true, List.of(
                        new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                "cannot have http:Caller param and return status code"))));
                return false;
            }
        }
        if (response.getType().isEnabledWithValue()) {
            Optional<TypeSymbol> type = semanticModel.types().getType(document, response.getType().getValue());
            if (hasHttpCaller && type.isPresent() && !type.get().subtypeOf(errorOrNilType)) {
                response.getType().setDiagnostics(new Diagnostics(true, List.of(
                        new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                "cannot have http:Caller param and return type"))));
                return false;
            }
        }
        if (Objects.nonNull(response.getBody()) && response.getBody().isEnabledWithValue()) {
            Optional<TypeSymbol> type = semanticModel.types().getType(document, response.getBody().getValue());
            if (hasHttpCaller && type.isPresent() && !type.get().subtypeOf(errorOrNilType)) {
                response.getBody().setDiagnostics(new Diagnostics(true, List.of(
                        new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                "cannot have http:Caller param and return body type"))));
                return false;
            }
        }
        Value statusCode = response.getStatusCode();
        if (Objects.nonNull(statusCode) && statusCode.isEnabledWithValue()) {
            if (hasHttpCaller) {
                response.setDiagnostics(new Diagnostics(true, List.of(
                        new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                "cannot have http:Caller param and return status code"))));
                return false;
            }
        }
        return true;
    }

    private boolean validateResourcePath(String path, List<Diagnostics.Info> diagnostics,
                                        Set<String> paramNames) {
        ResourcePathParser.ParseResult parseResult = ResourcePathParser.parseResourcePath(path);
        if (!parseResult.isValid()) {
            for (ResourcePathParser.ParseError error : parseResult.getErrors()) {
                diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, error.getMessage()));
            }
            return false;
        }

        boolean foundRestParam = false;
        for (ResourcePathParser.Segment segment: parseResult.getSegments()) {
            if (foundRestParam) {
                diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                        "cannot have path segments after rest parameter"));
                return false;
            }
            if (segment instanceof ResourcePathParser.RestParamSegment paramSegment) {
                foundRestParam = true;
                if (Objects.nonNull(paramSegment.getParamName())) {
                    if (!validPathParam(diagnostics, paramNames, paramSegment, identifierValidator)) {
                        return false;
                    }
                    continue;
                }
                String type = paramSegment.getTypeDescriptor();
                if (!validPathParamType(diagnostics, type, null)) {
                    return false;
                }
            } else if (segment instanceof ResourcePathParser.ParamSegment paramSegment) {
                if (!validPathParam(diagnostics, paramNames, paramSegment, identifierValidator)) {
                    return false;
                }
            } else if (segment instanceof ResourcePathParser.ValueSegment valueSegment) {
                String value = valueSegment.getValue();
                if (value.isEmpty()) {
                    diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                            "empty resource path segment"));
                    return false;
                }
                if (!validIdentifier(identifierValidator, value, diagnostics)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validPathParam(List<Diagnostics.Info> diagnostics, Set<String> paramNames,
                                   ResourcePathParser.ParamSegment paramSegment,
                                   IdentifierValidator identifierValidator) {
        String paramName = paramSegment.getParamName();
        String type = paramSegment.getTypeDescriptor();
        if (!validPathParamType(diagnostics, type, paramName)){
            return false;
        }

        if (!validIdentifier(identifierValidator, paramName, diagnostics)) {
            return false;
        }

        if (!paramNames.add(paramName)) {
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                    "duplicate parameter name: '" + paramName + "'"));
            return false;
        }
        return true;
    }

    private boolean validPathParamType(List<Diagnostics.Info> diagnostics, String type, String paramName) {
        if (!type.contains(":")) { // type is not coming from a different module
            Optional<TypeSymbol> paramTypeSymbol = semanticModel.types().getType(document, type);
            Diagnostics.Info errorMsg = new Diagnostics.Info(DiagnosticSeverity.ERROR,
                    paramName == null ? "invalid rest param type" : "invalid type for parameter: " + paramName);
            if (paramTypeSymbol.isPresent()) {
                if (!paramTypeSymbol.get().subtypeOf(basicType)) {
                    diagnostics.add(errorMsg);
                    return false;
                }
            } else {
                Node node = NodeParser.parseModuleMemberDeclaration("const CONST = " + type + ";");
                if (node instanceof ConstantDeclarationNode constNode && !constNode.initializer().isMissing()) {
                    Node initializer = constNode.initializer();
                    boolean isBasicLiteral = initializer instanceof BasicLiteralNode;
                    if (!isBasicLiteral) { // can be a const or enum
                        loadConstAndEnums();
                        boolean isConstReference = constantSymbols.stream().anyMatch(symbol -> symbol.nameEquals(type));
                        if (!isConstReference) {
                            boolean isEnumReference = enumSymbols.stream().anyMatch(
                                    enumSymbol -> enumSymbol.nameEquals(type) || enumSymbol.members()
                                            .stream().anyMatch(member -> member.nameEquals(type)));
                            if (!isEnumReference) {
                                diagnostics.add(errorMsg);
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean validQueryOrHeaderType(String type, TypeSymbol typeConstrain) {
        if (!type.contains(":")) { // type is not coming from a different module
            Optional<TypeSymbol> paramTypeSymbol = semanticModel.types().getType(document, type);
            if (paramTypeSymbol.isPresent()) {
                return paramTypeSymbol.get().subtypeOf(typeConstrain);
            } else {
                Node node = NodeParser.parseModuleMemberDeclaration("const CONST = " + type + ";");
                if (node instanceof ConstantDeclarationNode constNode && !constNode.initializer().isMissing()) {
                    Node initializer = constNode.initializer();
                    boolean isBasicLiteral = initializer instanceof BasicLiteralNode;
                    if (!isBasicLiteral) { // can be a const or enum
                        loadConstAndEnums();
                        boolean isConstReference = constantSymbols.stream().anyMatch(symbol -> symbol.nameEquals(type));
                        if (!isConstReference) {
                            return enumSymbols.stream().anyMatch(
                                    enumSymbol -> enumSymbol.nameEquals(type) || enumSymbol.members()
                                            .stream().anyMatch(member -> member.nameEquals(type)));
                        }
                    }
                }
            }
        }
        return true;
    }

    public static boolean validIdentifier(IdentifierValidator validator, String identifier,
                                    List<Diagnostics.Info> diagnostics) {
        if (!validator.isValidIdentifier(identifier)) {
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, validator.getErrorMessage()));
            return false;
        }
        if (KEYWORD_LIST.contains(identifier)) {
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                    "usage of reserved keyword: '" + identifier + "'"));
            return false;
        }
        return true;
    }
}
