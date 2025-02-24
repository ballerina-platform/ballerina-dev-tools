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

package io.ballerina.modelgenerator.commons;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Factory class to create {@link FunctionData} instances from function symbols.
 *
 * <p>
 * The class first checks if the item exists in the index. If not, it derives the symbol using the semantic model.
 * </p>
 *
 * @since 2.0.0
 */
public class FunctionDataBuilder {

    private SemanticModel semanticModel;
    private TypeSymbol errorTypeSymbol;
    private Package resolvedPackage;
    private FunctionSymbol functionSymbol;
    private FunctionData.Kind functionResultKind;
    private String functionName;
    private String description;
    private ModuleInfo moduleInfo;
    private ModuleInfo userModuleInfo;
    private String resourcePath;
    private ObjectTypeSymbol parentSymbol;
    private String parentSymbolType;

    public static final String REST_RESOURCE_PATH = "/path/to/subdirectory";
    public static final String REST_PARAM_PATH = "/path/to/resource";
    public static final String REST_RESOURCE_PATH_LABEL = "Remaining Resource Path";

    public FunctionDataBuilder semanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.errorTypeSymbol = semanticModel.types().ERROR;
        return this;
    }

    public FunctionDataBuilder resolvedPackage(Package resolvedPackage) {
        if (semanticModel == null) {
            semanticModel(resolvedPackage.getCompilation().getSemanticModel(
                    resolvedPackage.getDefaultModule().moduleId()));
        }
        this.resolvedPackage = resolvedPackage;
        return this;
    }

    public FunctionDataBuilder name(String name) {
        this.functionName = name;
        return this;
    }

    public FunctionDataBuilder moduleInfo(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
        return this;
    }

    public FunctionDataBuilder resourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    public FunctionDataBuilder functionSymbol(FunctionSymbol functionSymbol) {
        if (moduleInfo == null) {
            functionSymbol.getModule().ifPresent(module -> moduleInfo = ModuleInfo.from(module.id()));
        }
        if (functionResultKind == null) {
            functionResultKind = FunctionData.Kind.FUNCTION;
            if (functionSymbol.kind() == SymbolKind.METHOD) {
                List<Qualifier> qualifiers = functionSymbol.qualifiers();
                if (qualifiers.contains(Qualifier.REMOTE)) {
                    functionResultKind = FunctionData.Kind.REMOTE;
                } else if (qualifiers.contains(Qualifier.RESOURCE)) {
                    functionResultKind = FunctionData.Kind.RESOURCE;
                }
            }
        }
        this.functionSymbol = functionSymbol;
        return this;
    }

    public FunctionDataBuilder functionResultKind(FunctionData.Kind kind) {
        this.functionResultKind = kind;
        return this;
    }

    public FunctionDataBuilder userModuleInfo(ModuleInfo moduleInfo) {
        this.userModuleInfo = moduleInfo;
        return this;
    }

    public FunctionDataBuilder parentSymbolType(String parentSymbolType) {
        this.parentSymbolType = parentSymbolType;
        return this;
    }

    public FunctionDataBuilder parentSymbol(ObjectTypeSymbol parentSymbol) {
        if (moduleInfo == null) {
            parentSymbol.getModule().ifPresent(module -> moduleInfo = ModuleInfo.from(module.id()));
        }
        this.parentSymbol = parentSymbol;
        return this;
    }

    public FunctionDataBuilder parentSymbol(SemanticModel semanticModel, Document document, LinePosition position,
                                            String parentSymbolName) {
        Stream<Symbol> symbolStream = (document == null || position == null)
                ? semanticModel.moduleSymbols().parallelStream()
                : semanticModel.visibleSymbols(document, position).parallelStream();
        setParentSymbol(symbolStream, parentSymbolName);
        return this;
    }

    public FunctionDataBuilder parentSymbol(SemanticModel semanticModel, String parentSymbolName) {
        setParentSymbol(semanticModel.moduleSymbols().parallelStream(), parentSymbolName);
        return this;
    }

    private void setParentSymbol(Stream<Symbol> symbolStream, String parentSymbolName) {
        this.parentSymbol = symbolStream
                .filter(symbol -> symbol.kind() == SymbolKind.VARIABLE && symbol.nameEquals(parentSymbolName))
                .map(symbol -> CommonUtils.getRawType(((VariableSymbol) symbol).typeDescriptor()))
                .filter(typeSymbol -> typeSymbol instanceof ObjectTypeSymbol)
                .map(typeSymbol -> (ObjectTypeSymbol) typeSymbol).findFirst()
                .orElse(null);
    }

    public FunctionData build() {
        // The function name is required to build the FunctionResult
        if (this.functionName == null) {
            this.functionName = this.functionSymbol.getName()
                    .orElseThrow(() -> new IllegalStateException("Function name not found"));
        }

        // The module information is required to build the FunctionResult
        if (moduleInfo == null) {
            throw new IllegalStateException("Module information not found");
        }

        // Defaulting the function result kind to FUNCTION if not provided
        if (functionResultKind == null) {
            functionResultKind = FunctionData.Kind.FUNCTION;
        }

        // Check if the function is in the index
        Optional<FunctionData> indexedResult = getFunctionFromIndex();
        if (indexedResult.isPresent()) {
            return indexedResult.get();
        }

        // Fetch the semantic model if not provided
        if (semanticModel == null) {
            semanticModel(PackageUtil.getSemanticModel(moduleInfo.org(), moduleInfo.packageName(), moduleInfo.version())
                    .orElseThrow(() -> new IllegalStateException("Semantic model not found")));
        }

        // Find the symbol if not provided
        if (functionSymbol == null) {
            // If the parent symbol is not found, and its name is provided, search for the parent symbol
            if (parentSymbol == null && parentSymbolType != null) {
                ObjectTypeSymbol fetchedParentTypeSymbol = semanticModel.moduleSymbols().parallelStream()
                        .filter(moduleSymbol -> moduleSymbol.nameEquals(parentSymbolType) &&
                                moduleSymbol instanceof ObjectTypeSymbol)
                        .map(moduleSymbol -> (ObjectTypeSymbol) moduleSymbol)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Parent symbol not found"));
                parentSymbol(fetchedParentTypeSymbol);
            }

            // If the parent symbol is not found, search for functions in the module-level
            if (parentSymbol == null) {
                FunctionSymbol fetchedSymbol = semanticModel.moduleSymbols().parallelStream()
                        .filter(moduleSymbol -> moduleSymbol.nameEquals(functionName) &&
                                moduleSymbol instanceof FunctionSymbol)
                        .map(moduleSymbol -> (FunctionSymbol) moduleSymbol)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Function symbol not found"));
                functionSymbol(fetchedSymbol);
            } else {
                // Fetch the init method if it is a connection
                if (functionResultKind == FunctionData.Kind.CONNECTOR) {
                    if (parentSymbol.kind() != SymbolKind.CLASS ||
                            !parentSymbol.qualifiers().contains(Qualifier.CLIENT)) {
                        throw new IllegalStateException("The connector should be a client class");
                    }
                    functionSymbol = ((ClassSymbol) parentSymbol).initMethod().orElseThrow(
                            () -> new IllegalStateException("Init method not found"));
                } else {
                    if (functionResultKind == FunctionData.Kind.RESOURCE) {
                        // TODO: Need to improve how resource path is stored in the codedata, as this should reflect
                        //  to the key in the methods map for easier retrieval.

                        // We need to identify the resource method through its signature since we cannot use the
                        // methods map. This limitation occurs because the resource path in the codedata is
                        // normalized for display, making it impossible to extract the correct key.
                        functionSymbol = parentSymbol.methods().values().parallelStream()
                                .filter(symbol -> symbol.kind() == SymbolKind.RESOURCE_METHOD &&
                                        symbol.nameEquals(functionName) &&
                                        buildResourcePathTemplate(symbol).resourcePathTemplate()
                                                .equals(resourcePath))
                                .findFirst()
                                .orElse(null);
                    } else {
                        // Fetch the respective method using the function name
                        functionSymbol = parentSymbol.methods().get(functionName);
                    }
                    if (functionSymbol == null) {
                        throw new IllegalStateException("Function symbol not found");
                    }
                }
            }
        }

        if (description == null) {
            // Set the description of the client class if it is a connector, Else, use the function itself
            Documentable documentable =
                    functionResultKind == FunctionData.Kind.CONNECTOR ? (ClassSymbol) parentSymbol : functionSymbol;
            this.description = documentable.documentation().flatMap(Documentation::description).orElse("");

        }

        // Obtain the return type of the function
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        String returnType = functionTypeSymbol.returnTypeDescriptor()
                .map(typeSymbol -> {
                    if (functionResultKind == FunctionData.Kind.CONNECTOR) {
                        return CommonUtils.getClassType(moduleInfo.packageName(),
                                parentSymbol.getName().orElse("Client"));
                    }
                    return getTypeSignature(typeSymbol, true);
                })
                .orElse("");

        ParamForTypeInfer paramForTypeInfer = null;
        if (functionSymbol.external()) {
            List<String> paramNameList = new ArrayList<>();
            functionTypeSymbol.params().ifPresent(paramList -> paramList
                    .stream()
                    .filter(paramSym -> paramSym.paramKind() == ParameterKind.DEFAULTABLE)
                    .forEach(paramSymbol -> paramNameList.add(paramSymbol.getName().orElse(""))));

            Map<String, TypeSymbol> returnTypeMap =
                    allMembers(functionTypeSymbol.returnTypeDescriptor().orElse(null));
            for (String paramName : paramNameList) {
                if (returnTypeMap.containsKey(paramName)) {
                    returnType = "json";
                    String defaultValue =
                            DefaultValueGeneratorUtil.getDefaultValueForType(returnTypeMap.get(paramName));
                    paramForTypeInfer = new ParamForTypeInfer(paramName, defaultValue, returnType);
                    break;
                }
            }
        }

        boolean returnError = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol)).orElse(false);

        ResourcePathTemplate resourcePathTemplate = null;
        if (functionResultKind == FunctionData.Kind.RESOURCE) {
            resourcePathTemplate = buildResourcePathTemplate(functionSymbol
            );
        }
        resourcePath = resourcePathTemplate == null ? "" : resourcePathTemplate.resourcePathTemplate();
        Map<String, ParameterData> parameters = new LinkedHashMap<>();

        // Store the resource path params
        if (resourcePathTemplate != null) {
            resourcePathTemplate.pathParams().forEach(param -> parameters.put(param.name(), param));
        }

        FunctionData functionData =
                new FunctionData(0, functionName, description, returnType, moduleInfo.packageName(),
                        moduleInfo.org(), moduleInfo.version(), resourcePath, FunctionData.Kind.FUNCTION, returnError,
                        paramForTypeInfer != null);

        Map<String, String> documentationMap =
                functionSymbol.documentation().map(Documentation::parameterMap).orElse(Map.of());
        ParamForTypeInfer finalParamForTypeInfer = paramForTypeInfer;
        functionTypeSymbol.params()
                .ifPresent(paramList -> paramList.forEach(
                        paramSymbol -> parameters.putAll(
                                getParameters(paramSymbol, documentationMap, finalParamForTypeInfer))));
        functionTypeSymbol.restParam()
                .ifPresent(paramSymbol -> parameters.putAll(
                        getParameters(paramSymbol, documentationMap, finalParamForTypeInfer)));
        functionData.setParameters(parameters);
        return functionData;
    }

    private Optional<FunctionData> getFunctionFromIndex() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionData> optFunctionResult =
                dbManager.getFunction(moduleInfo.org(), moduleInfo.packageName(), functionName, functionResultKind,
                        resourcePath);
        if (optFunctionResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionData functionData = optFunctionResult.get();
        LinkedHashMap<String, ParameterData> parameters =
                dbManager.getFunctionParametersAsMap(functionData.functionId());
        functionData.setParameters(parameters);
        return Optional.of(functionData);
    }

    private Map<String, ParameterData> getParameters(ParameterSymbol paramSymbol,
                                                     Map<String, String> documentationMap,
                                                     ParamForTypeInfer paramForTypeInfer) {
        Map<String, ParameterData> parameters = new LinkedHashMap<>();
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterData.Kind parameterKind = ParameterData.Kind.fromKind(paramSymbol.paramKind());
        String paramType;
        boolean optional = true;
        String defaultValue;
        TypeSymbol typeSymbol = paramSymbol.typeDescriptor();
        String importStatements = CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null);
        if (parameterKind == ParameterData.Kind.REST_PARAMETER) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
            paramType = getTypeSignature(((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
        } else if (parameterKind == ParameterData.Kind.INCLUDED_RECORD) {
            paramType = getTypeSignature(typeSymbol);
            Map<String, ParameterData> includedParameters =
                    getIncludedRecordParams((RecordTypeSymbol) CommonUtil.getRawType(typeSymbol), true,
                            new HashMap<>());
            parameters.putAll(includedParameters);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        } else if (parameterKind == ParameterData.Kind.REQUIRED) {
            paramType = getTypeSignature(typeSymbol);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            optional = false;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    parameters.put(paramName, ParameterData.from(paramName, paramDescription, paramType, defaultValue,
                            ParameterData.Kind.PARAM_FOR_TYPE_INFER, optional, importStatements));
                    return parameters;
                }
            }
            defaultValue = getParamDefaultValue(paramSymbol, typeSymbol);
            paramType = getTypeSignature(typeSymbol);
        }
        parameters.put(paramName, ParameterData.from(paramName, paramDescription, paramType, defaultValue,
                parameterKind, optional, importStatements));
        return parameters;
    }

    private Map<String, ParameterData> getIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                               boolean insert,
                                                               Map<String, String> documentationMap) {
        Map<String, ParameterData> parameters = new LinkedHashMap<>();
        recordTypeSymbol.typeInclusions().forEach(includedType -> parameters.putAll(
                getIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(includedType), insert,
                        documentationMap))
        );
        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol typeSymbol = recordFieldSymbol.typeDescriptor();
            TypeSymbol fieldType = CommonUtil.getRawType(typeSymbol);
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
            String paramDescription = entry.getValue().documentation()
                    .flatMap(Documentation::description).orElse("");
            if (documentationMap.containsKey(paramName) && !paramDescription.isEmpty()) {
                documentationMap.put(paramName, paramDescription);
            } else if (!documentationMap.containsKey(paramName)) {
                documentationMap.put(paramName, paramDescription);
            }
            if (!insert) {
                continue;
            }

            String defaultValue = getRecordFieldDefaultValue(recordFieldSymbol, fieldType);
            String paramType = getTypeSignature(typeSymbol);
            boolean optional = recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue();
            parameters.put(paramName, ParameterData.from(paramName, documentationMap.get(paramName),
                    paramType, defaultValue, ParameterData.Kind.INCLUDED_FIELD, optional,
                    CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null)));
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = getTypeSignature(typeSymbol);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            parameters.put("Additional Values", ParameterData.from("Additional Values",
                    "Capture key value pairs", paramType, defaultValue,
                    ParameterData.Kind.INCLUDED_RECORD_REST, true,
                    CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null)));
        });
        return parameters;
    }

    private String getParamDefaultValue(ParameterSymbol paramSymbol, TypeSymbol typeSymbol) {
        String defaultValue;
        Location symbolLocation = paramSymbol.getLocation().get();
        Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
        defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        if (document != null) {
            defaultValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                    symbolLocation, resolvedPackage.packageName().value());
        }
        return defaultValue;
    }

    private String getRecordFieldDefaultValue(RecordFieldSymbol recordFieldSymbol, TypeSymbol fieldType) {
        Location symbolLocation = recordFieldSymbol.getLocation().get();
        Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
        String defaultValue;
        if (document != null) {
            defaultValue = getAttributeDefaultValue(document.syntaxTree().rootNode(),
                    symbolLocation, resolvedPackage.packageName().value());
            if (defaultValue == null) {
                defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
            }
        } else {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
        }
        return defaultValue;
    }

    private static String getParamDefaultValue(ModulePartNode rootNode, Location location, String module) {
        NonTerminalNode node = rootNode.findNode(TextRange.from(location.textRange().startOffset(),
                location.textRange().length()));
        if (node.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
            DefaultableParameterNode valueNode = (DefaultableParameterNode) node;
            ExpressionNode expression = (ExpressionNode) valueNode.expression();
            if (expression instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
                return module + ":" + simpleNameReferenceNode.name().text();
            } else if (expression instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) {
                return qualifiedNameReferenceNode.modulePrefix().text() + ":" + qualifiedNameReferenceNode.identifier()
                        .text();
            } else {
                return expression.toSourceCode();
            }
        }
        return null;
    }

    private static String getAttributeDefaultValue(ModulePartNode rootNode, Location location, String module) {
        NonTerminalNode node = rootNode.findNode(TextRange.from(location.textRange().startOffset(),
                location.textRange().length()));
        if (node.kind() == SyntaxKind.RECORD_FIELD_WITH_DEFAULT_VALUE) {
            RecordFieldWithDefaultValueNode valueNode = (RecordFieldWithDefaultValueNode) node;
            ExpressionNode expression = valueNode.expression();
            if (expression instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
                return module + ":" + simpleNameReferenceNode.name().text();
            } else if (expression instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) {
                return qualifiedNameReferenceNode.modulePrefix().text() + ":" + qualifiedNameReferenceNode.identifier()
                        .text();
            } else {
                return expression.toSourceCode();
            }
        }
        return null;
    }

    private Document findDocument(Package pkg, String path) {
        if (resolvedPackage == null) {
            return null;
        }
        Project project = pkg.project();
        Module defaultModule = pkg.getDefaultModule();
        String module = pkg.packageName().value();
        Path docPath = project.sourceRoot().resolve("modules").resolve(module).resolve(path);
        try {
            DocumentId documentId = project.documentId(docPath);
            return defaultModule.document(documentId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static Map<String, TypeSymbol> allMembers(TypeSymbol typeSymbol) {
        Map<String, TypeSymbol> members = new HashMap<>();
        if (typeSymbol == null) {
            return members;
        } else if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            unionTypeSymbol.memberTypeDescriptors()
                    .forEach(memberType -> members.put(memberType.getName().orElse(""), memberType));
        } else if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
            intersectionTypeSymbol.memberTypeDescriptors()
                    .forEach(memberType -> members.put(memberType.getName().orElse(""), memberType));
        } else {
            members.put(typeSymbol.getName().orElse(""), typeSymbol);
        }
        return members;
    }

    private ResourcePathTemplate buildResourcePathTemplate(FunctionSymbol functionSymbol) {
        Map<String, String> documentationMap = functionSymbol.documentation().map(Documentation::parameterMap)
                .orElse(Map.of());
        StringBuilder pathBuilder = new StringBuilder();
        ResourceMethodSymbol resourceMethodSymbol = (ResourceMethodSymbol) functionSymbol;
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        List<ParameterData> pathParams = new ArrayList<>();
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                for (Symbol pathSegment : pathSegmentList.list()) {
                    pathBuilder.append("/");
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        String defaultValue = DefaultValueGeneratorUtil
                                .getDefaultValueForType(pathParameterSymbol.typeDescriptor());
                        String type = CommonUtils.getTypeSignature(semanticModel, pathParameterSymbol.typeDescriptor(),
                                true);
                        String paramName = pathParameterSymbol.getName().orElse("");
                        String paramDescription = documentationMap.get(paramName);
                        pathBuilder.append("[").append(paramName).append("]");
                        pathParams.add(
                                ParameterData.from(paramName, type, ParameterData.Kind.PATH_PARAM, defaultValue,
                                        paramDescription, false));
                    } else {
                        pathBuilder.append(pathSegment.getName().orElse(""));
                    }
                }
                ((PathSegmentList) resourcePath).pathRestParameter().ifPresent(pathRestParameter -> {
                    pathParams.add(
                            ParameterData.from(REST_RESOURCE_PATH_LABEL, "string",
                                    ParameterData.Kind.PATH_REST_PARAM, REST_PARAM_PATH, REST_RESOURCE_PATH_LABEL,
                                    false));
                });
            }
            case PATH_REST_PARAM -> {
                pathBuilder.append(REST_RESOURCE_PATH);
            }
            case DOT_RESOURCE_PATH -> pathBuilder.append("/");
        }
        return new ResourcePathTemplate(pathBuilder.toString(), pathParams);
    }

    public record ResourcePathTemplate(String resourcePathTemplate, List<ParameterData> pathParams) {
    }

    private String getTypeSignature(TypeSymbol typeSymbol) {
        return getTypeSignature(typeSymbol, false);
    }

    private String getTypeSignature(TypeSymbol typeSymbol, boolean ignoreError) {
        if (userModuleInfo == null) {
            return CommonUtils.getTypeSignature(semanticModel, typeSymbol, ignoreError);
        }
        return CommonUtils.getTypeSignature(semanticModel, typeSymbol, ignoreError, userModuleInfo);
    }

    private record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }
}
