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

import io.ballerina.centralconnector.CentralAPI;
import io.ballerina.centralconnector.RemoteCentral;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.TypeBuilder;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.LSClientLogger;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.eclipse.lsp4j.MessageType;

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

    public static final String DISPLAY_ANNOTATION = "display";
    public static final String LABEL = "label";
    private SemanticModel semanticModel;
    private TypeSymbol errorTypeSymbol;
    private Package resolvedPackage;
    private FunctionSymbol functionSymbol;
    private FunctionData.Kind functionKind;
    private String functionName;
    private String description;
    private ModuleInfo moduleInfo;
    private ModuleInfo userModuleInfo;
    private String resourcePath;
    private ObjectTypeSymbol parentSymbol;
    private String parentSymbolType;
    private LSClientLogger lsClientLogger;
    private Project project;
    private boolean isCurrentModule;

    public static final String REST_RESOURCE_PATH = "/path/to/subdirectory";
    public static final String REST_PARAM_PATH = "/path/to/resource";
    public static final String REST_RESOURCE_PATH_LABEL = "Remaining Resource Path";

    // Package resolution messages
    private static final String PULLING_THE_MODULE_MESSAGE = "Pulling the module '%s' from the central";
    private static final String MODULE_PULLING_FAILED_MESSAGE = "Failed to pull the module: %s";
    private static final String MODULE_PULLING_SUCCESS_MESSAGE = "Successfully pulled the module: %s";

    private static final String CLIENT_SYMBOL = "Client";

    public FunctionDataBuilder semanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.errorTypeSymbol = semanticModel.types().ERROR;
        return this;
    }

    public FunctionDataBuilder resolvedPackage(Package resolvedPackage) {
        if (semanticModel == null) {
            semanticModel(PackageUtil.getCompilation(resolvedPackage).getSemanticModel(
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
        if (functionKind == null) {
            functionKind = getFunctionKind(functionSymbol);
        }
        this.functionSymbol = functionSymbol;
        return this;
    }

    public FunctionDataBuilder functionResultKind(FunctionData.Kind kind) {
        this.functionKind = kind;
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

    public FunctionDataBuilder lsClientLogger(LSClientLogger lsClientLogger) {
        this.lsClientLogger = lsClientLogger;
        return this;
    }

    public FunctionDataBuilder project(Project project) {
        this.project = project;
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
            if (functionSymbol == null) {
                throw new IllegalStateException("Function symbol must be provided if function name is not given");
            }
            this.functionName = this.functionSymbol.getName()
                    .orElseThrow(() -> new IllegalStateException("Function name not found"));
        }

        // The module information is required to build the FunctionResult
        if (moduleInfo == null) {
            throw new IllegalStateException("Module information not found");
        }

        // Check if this is a local symbol
        isCurrentModule = userModuleInfo != null && (!moduleInfo.isComplete() || userModuleInfo.equals(moduleInfo));

        // Defaulting the function result kind to FUNCTION if not provided
        if (functionKind == null) {
            functionKind = FunctionData.Kind.FUNCTION;
        }

        checkLocalModule();

        // Check if the package is pulled
        if (semanticModel == null) {
            if (moduleInfo.version() == null) {
                CentralAPI centralApi = RemoteCentral.getInstance();
                moduleInfo = new ModuleInfo(moduleInfo.org(), moduleInfo.packageName(), moduleInfo.moduleName(),
                        centralApi.latestPackageVersion(moduleInfo.org(), moduleInfo.packageName()));
            }

            if (moduleInfo.isComplete() &&
                    PackageUtil.isModuleUnresolved(moduleInfo.org(), moduleInfo.packageName(), moduleInfo.version())) {
                notifyClient(MessageType.Info, PULLING_THE_MODULE_MESSAGE);
                if (semanticModel == null) {
                    deriveSemanticModel();
                }
                if (semanticModel == null) {
                    notifyClient(MessageType.Error, MODULE_PULLING_FAILED_MESSAGE);
                } else {
                    notifyClient(MessageType.Info, MODULE_PULLING_SUCCESS_MESSAGE);
                }
            }
        }

        // Check if the function is in the index
        Optional<FunctionData> indexedResult = getFunctionFromIndex();
        if (indexedResult.isPresent()) {
            return indexedResult.get();
        }

        // Fetch the semantic model if not provided
        if (semanticModel == null) {
            deriveSemanticModel();
        }

        // Find the symbol if not provided
        if (functionSymbol == null) {
            // If the parent symbol is not found, and its name is provided, search for the parent symbol
            if (parentSymbol == null && parentSymbolType != null) {
                setParentSymbol();
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
                if (functionKind == FunctionData.Kind.CONNECTOR) {
                    if (parentSymbol.kind() != SymbolKind.CLASS ||
                            !parentSymbol.qualifiers().contains(Qualifier.CLIENT)) {
                        throw new IllegalStateException("The connector should be a client class");
                    }
                    ClassSymbol classSymbol = (ClassSymbol) parentSymbol;
                    Optional<MethodSymbol> initMethod = classSymbol.initMethod();

                    // If the init method is not found, create the function data without parameters
                    if (initMethod.isEmpty()) {
                        String clientName = getFunctionName();
                        FunctionData functionData = new FunctionData(0, clientName, getDescription(classSymbol),
                                getTypeSignature(clientName), moduleInfo.packageName(), moduleInfo.org(),
                                moduleInfo.version(), "", functionKind, false, false, null);
                        functionData.setParameters(Map.of());
                        return functionData;
                    }

                    functionSymbol = initMethod.get();
                } else {
                    if (functionKind == FunctionData.Kind.RESOURCE) {
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
                    } else if (functionKind == FunctionData.Kind.CLASS_INIT) {
                        if (parentSymbol.kind() != SymbolKind.CLASS) {
                            throw new IllegalStateException("Parent symbol should be a class symbol");
                        }
                        ClassSymbol classSymbol = (ClassSymbol) parentSymbol;
                        Optional<MethodSymbol> initMethod = classSymbol.initMethod();
                        initMethod.ifPresent(methodSymbol -> functionSymbol = methodSymbol);
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
                    functionKind == FunctionData.Kind.CONNECTOR ? (ClassSymbol) parentSymbol : functionSymbol;
            this.description = getDescription(documentable);
        }

        // Obtain the return information of the function
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        ReturnData returnData = getReturnData(functionSymbol);
        ParamForTypeInfer paramForTypeInfer = returnData.paramForTypeInfer();

        // Store the resource path params
        Map<String, ParameterData> parameters = new LinkedHashMap<>();
        if (functionKind == FunctionData.Kind.RESOURCE) {
            ResourcePathTemplate resourcePathTemplate = buildResourcePathTemplate(functionSymbol);
            resourcePath = resourcePathTemplate.resourcePathTemplate();
            resourcePathTemplate.pathParams().forEach(param -> parameters.put(param.name(), param));
        }

        FunctionData functionData = new FunctionData(0, getFunctionName(), description, returnData.returnType(),
                moduleInfo.packageName(), moduleInfo.org(), moduleInfo.version(), resourcePath, functionKind,
                returnData.returnError(), paramForTypeInfer != null, returnData.importStatements());

        Types types = semanticModel.types();
        TypeBuilder builder = semanticModel.types().builder();
        UnionTypeSymbol union = builder.UNION_TYPE.withMemberTypes(types.BOOLEAN, types.NIL, types.STRING, types.INT,
                types.FLOAT, types.DECIMAL, types.BYTE, types.REGEX, types.XML).build();

        Map<String, String> documentationMap =
                functionSymbol.documentation().map(Documentation::parameterMap).orElse(Map.of());
        functionTypeSymbol.params().ifPresent(paramList -> paramList.forEach(paramSymbol -> parameters.putAll(
                getParameters(paramSymbol, documentationMap, paramForTypeInfer, union))));
        functionTypeSymbol.restParam().ifPresent(paramSymbol -> parameters.putAll(
                getParameters(paramSymbol, documentationMap, paramForTypeInfer, union)));
        functionData.setParameters(parameters);
        return functionData;
    }

    private void checkLocalModule() {
        if (project != null && moduleInfo != null && isLocal()) {
            for (Module module : project.currentPackage().modules()) {
                ModuleName moduleName = module.moduleName();
                if ((moduleName.packageName() + "." + moduleName.moduleNamePart()).equals(moduleInfo.moduleName())) {
                    semanticModel(PackageUtil.getCompilation(project).getSemanticModel(module.moduleId()));
                    break;
                }
            }
        }
    }

    public boolean isLocal() {
        if (project != null && moduleInfo != null) {
            PackageDescriptor descriptor = project.currentPackage().descriptor();
            return moduleInfo.org().equals(descriptor.org().value()) &&
                    moduleInfo.packageName().startsWith(descriptor.name().value());
        }
        return false;
    }

    private ReturnData getReturnData(FunctionSymbol symbol) {
        FunctionTypeSymbol functionTypeSymbol = symbol.typeDescriptor();
        Optional<TypeSymbol> returnTypeSymbol = functionTypeSymbol.returnTypeDescriptor();
        String returnType = returnTypeSymbol
                .map(typeSymbol -> {
                    if (functionKind == FunctionData.Kind.CONNECTOR || functionKind == FunctionData.Kind.CLASS_INIT) {
                        return CommonUtils.getClassType(moduleInfo.packageName(),
                                parentSymbol.getName().orElse("Client"));
                    }
                    return getTypeSignature(typeSymbol, true);
                }).orElse("");

        ParamForTypeInfer paramForTypeInfer = null;
        if (symbol.external()) {
            List<String> paramNameList = new ArrayList<>();
            functionTypeSymbol.params().ifPresent(paramList -> paramList
                    .stream()
                    .filter(paramSym -> paramSym.paramKind() == ParameterKind.DEFAULTABLE)
                    .forEach(paramSymbol -> paramNameList.add(paramSymbol.getName().orElse(""))));

            Map<String, TypeSymbol> returnTypeMap = new HashMap<>();
            returnTypeSymbol.ifPresent(typeSymbol -> allMembers(returnTypeMap, typeSymbol));
            for (String paramName : paramNameList) {
                if (returnTypeMap.containsKey(paramName)) {
                    TypeSymbol typeDescriptor = returnTypeMap.get(paramName);
                    String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeDescriptor);
                    paramForTypeInfer = new ParamForTypeInfer(paramName, defaultValue,
                            CommonUtils.getTypeSignature(semanticModel, CommonUtils.getRawType(typeDescriptor), true));
                    break;
                }
            }
        }

        String importStatements = returnTypeSymbol.map(
                typeSymbol -> getImportStatements(returnTypeSymbol.get())).orElse(null);

        boolean returnError = returnTypeSymbol
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol)).orElse(false);
        return new ReturnData(returnType, paramForTypeInfer, returnError, importStatements);
    }

    private void setParentSymbol() {
        if (semanticModel == null) {
            deriveSemanticModel();
        }
        ObjectTypeSymbol fetchedParentTypeSymbol = semanticModel.moduleSymbols().parallelStream()
                .filter(moduleSymbol -> moduleSymbol.nameEquals(parentSymbolType) &&
                        moduleSymbol instanceof ObjectTypeSymbol)
                .map(moduleSymbol -> (ObjectTypeSymbol) moduleSymbol)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Parent symbol not found"));
        parentSymbol(fetchedParentTypeSymbol);
    }

    private FunctionData.Kind getFunctionKind(FunctionSymbol symbol) {
        if (symbol.kind() == SymbolKind.METHOD) {
            List<Qualifier> qualifiers = symbol.qualifiers();
            if (qualifiers.contains(Qualifier.REMOTE)) {
                return FunctionData.Kind.REMOTE;
            }
            if (qualifiers.contains(Qualifier.RESOURCE)) {
                return FunctionData.Kind.RESOURCE;
            }
        }
        if (symbol.kind() == SymbolKind.RESOURCE_METHOD) {
            return FunctionData.Kind.RESOURCE;
        }
        return FunctionData.Kind.FUNCTION;
    }

    private void deriveSemanticModel() {
        semanticModel(PackageUtil.getSemanticModel(moduleInfo.org(), moduleInfo.packageName(), moduleInfo.version())
                .orElseThrow(() -> new IllegalStateException("Semantic model not found")));
    }

    public List<FunctionData> buildChildNodes() {
        if (parentSymbolType != null && moduleInfo != null) {
            List<FunctionData> fetchedMethods = getMethodsFromIndex();
            if (!fetchedMethods.isEmpty()) {
                return fetchedMethods;
            }
        }

        // The parent symbol must be present
        if (this.parentSymbol == null && this.parentSymbolType == null) {
            throw new IllegalStateException("Parent symbol must be provided");
        }

        checkLocalModule();

        // Derive if the semantic model is not provided
        if (semanticModel == null) {
            deriveSemanticModel();
        }

        // Derive if the parent symbol is not provided
        if (this.parentSymbol == null) {
            setParentSymbol();
        }

        // The parent symbol should be a class symbol
        if (this.parentSymbol.kind() != SymbolKind.CLASS) {
            throw new IllegalStateException("Parent symbol should be a class symbol");
        }

        ClassSymbol classSymbol = (ClassSymbol) this.parentSymbol;
        List<FunctionData> functionDataList = new ArrayList<>();
        for (MethodSymbol methodSymbol : classSymbol.methods().values()) {
            List<Qualifier> qualifiers = methodSymbol.qualifiers();
            if (qualifiers.contains(Qualifier.PRIVATE)) {
                continue;
            }
            FunctionData.Kind methodKind = getFunctionKind(methodSymbol);
            if (methodKind == FunctionData.Kind.FUNCTION && !qualifiers.contains(Qualifier.PUBLIC)) {
                continue;
            }
            ReturnData returnData = getReturnData(methodSymbol);

            // If the method is a resource method, the resource path should be derived
            String methodResourcePath;
            if (methodKind == FunctionData.Kind.RESOURCE) {
                ResourcePathTemplate resourcePathTemplate = buildResourcePathTemplate(methodSymbol);
                methodResourcePath = resourcePathTemplate.resourcePathTemplate();
            } else {
                methodResourcePath = "";
            }

            FunctionData functionData = new FunctionData(0,
                    methodSymbol.getName().orElse(""),
                    getDescription(methodSymbol),
                    returnData.returnType(),
                    moduleInfo.packageName(),
                    moduleInfo.org(),
                    moduleInfo.version(),
                    methodResourcePath,
                    methodKind,
                    returnData.returnError(),
                    returnData.paramForTypeInfer() != null,
                    returnData.importStatements());
            functionDataList.add(functionData);
        }
        return functionDataList;
    }

    private Optional<FunctionData> getFunctionFromIndex() {
        DatabaseManager dbManager = DatabaseManager.getInstance();

        // Skipping the index since we currently only index connectors with the name "Client".
        // TODO: This should be removed after the package index is revamped.
        if (parentSymbolType != null && !parentSymbolType.isEmpty() && !CLIENT_SYMBOL.equals(parentSymbolType)) {
            return Optional.empty();
        }

        Optional<FunctionData> optFunctionResult =
                dbManager.getFunction(moduleInfo.org(), moduleInfo.packageName(), getFunctionName(),
                        functionKind, resourcePath);
        if (optFunctionResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionData functionData = optFunctionResult.get();
        LinkedHashMap<String, ParameterData> parameters =
                dbManager.getFunctionParametersAsMap(functionData.functionId());
        functionData.setParameters(parameters);
        return Optional.of(functionData);
    }

    private List<FunctionData> getMethodsFromIndex() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        List<FunctionData> methods = dbManager.getMethods(parentSymbolType, moduleInfo.org(), moduleInfo.packageName());
        if (methods.isEmpty()) {
            return new ArrayList<>();
        }
        methods.parallelStream()
                .forEach(method -> method.setParameters(dbManager.getFunctionParametersAsMap(method.functionId())));
        return methods;
    }

    private Map<String, ParameterData> getParameters(ParameterSymbol paramSymbol,
                                                     Map<String, String> documentationMap,
                                                     ParamForTypeInfer paramForTypeInfer,
                                                     UnionTypeSymbol union) {
        Map<String, ParameterData> parameters = new LinkedHashMap<>();
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterData.Kind parameterKind = ParameterData.Kind.fromKind(paramSymbol.paramKind());
        Object paramType;
        boolean optional = true;
        String defaultValue;
        TypeSymbol typeSymbol = paramSymbol.typeDescriptor();
        String importStatements = getImportStatements(typeSymbol);
        if (parameterKind == ParameterData.Kind.REST_PARAMETER) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
            paramType = getTypeSignature(((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
        } else if (parameterKind == ParameterData.Kind.INCLUDED_RECORD) {
            Map<String, String> includedRecordParamDocs = new HashMap<>();
            if (typeSymbol.getModule().isPresent() && typeSymbol.getName().isPresent()) {
                ModuleID id = typeSymbol.getModule().get().id();
                if (semanticModel != null) {
                    Optional<Symbol> typeByName = semanticModel.types().getTypeByName(id.orgName(), id.moduleName(),
                            "", typeSymbol.getName().get());
                    if (typeByName.isPresent() && typeByName.get() instanceof TypeDefinitionSymbol typeDefSymbol) {
                        Optional<Documentation> documentation = typeDefSymbol.documentation();
                        documentation.ifPresent(documentation1 -> includedRecordParamDocs.putAll(
                                documentation1.parameterMap()));
                    }
                }
            }
            paramType = getTypeSignature(typeSymbol);
            Map<String, ParameterData> includedParameters = getIncludedRecordParams(
                    (RecordTypeSymbol) CommonUtil.getRawType(typeSymbol), true, includedRecordParamDocs, union);
            parameters.putAll(includedParameters);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        } else if (parameterKind == ParameterData.Kind.REQUIRED) {
            if (isAgentModelType(paramName)) {
                List<String> memberTypes = new ArrayList<>();
                TypeSymbol rawParamType = CommonUtils.getRawType(typeSymbol);
                if (rawParamType.typeKind() == TypeDescKind.UNION) {
                    UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) rawParamType;
                    for (TypeSymbol memType : unionTypeSymbol.userSpecifiedMemberTypes()) {
                        memberTypes.add(memType.signature());
                    }
                    paramType = memberTypes;
                } else {
                    paramType = getTypeSignature(typeSymbol);
                }
            } else {
                paramType = getTypeSignature(typeSymbol);
            }
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            optional = false;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    parameters.put(paramName,
                            ParameterData.from(paramName, paramDescription, paramType, defaultValue,
                                    ParameterData.Kind.PARAM_FOR_TYPE_INFER, optional, importStatements));
                    return parameters;
                }
            }
            defaultValue = getDefaultValue(paramSymbol, typeSymbol);
            paramType = getTypeSignature(typeSymbol);
        }
        ParameterData parameterData = ParameterData.from(paramName, paramDescription,
                getLabel(paramSymbol.annotAttachments()), paramType, defaultValue, parameterKind, optional,
                importStatements);
        parameters.put(paramName, parameterData);
        addParameterMemberTypes(typeSymbol, parameterData, union);
        return parameters;
    }

    private static void addParameterMemberTypes(TypeSymbol typeSymbol, ParameterData parameterData,
                                                UnionTypeSymbol union) {

        if (typeSymbol instanceof UnionTypeSymbol unionTypeSymbol) {
            unionTypeSymbol.memberTypeDescriptors().forEach(
                    memberType -> addParameterMemberTypes(memberType, parameterData, union));
            return;
        }

        String packageIdentifier = "";
        ModuleInfo moduleInfo = null;
        if (typeSymbol.getModule().isPresent()) {
            ModuleID id = typeSymbol.getModule().get().id();
            packageIdentifier = "%s:%s:%s".formatted(id.orgName(), id.moduleName(), id.version());
            moduleInfo = ModuleInfo.from(id);
        }
        String type = CommonUtils.getTypeSignature(typeSymbol, moduleInfo);
        String kind = "OTHER";
        TypeSymbol rawType = CommonUtils.getRawType(typeSymbol);
        if (typeSymbol.subtypeOf(union)) {
            kind = "BASIC_TYPE";
        } else if (rawType instanceof TupleTypeSymbol) {
            kind = "TUPLE_TYPE";
        } else if (rawType instanceof ArrayTypeSymbol arrayTypeSymbol) {
            kind = "ARRAY_TYPE";
            TypeSymbol memberType = arrayTypeSymbol.memberTypeDescriptor();
            if (memberType.getModule().isPresent()) {
                ModuleID id = memberType.getModule().get().id();
                packageIdentifier = "%s:%s:%s".formatted(id.orgName(), id.moduleName(), id.version());
                moduleInfo = ModuleInfo.from(id);
            }
            type = CommonUtils.getTypeSignature(memberType, moduleInfo);
        } else if (rawType instanceof RecordTypeSymbol) {
            if (typeSymbol instanceof RecordTypeSymbol) {
                kind = "ANON_RECORD_TYPE";
            } else {
                kind = "RECORD_TYPE";
            }
        } else if (rawType instanceof MapTypeSymbol mapTypeSymbol) {
            kind = "MAP_TYPE";
            TypeSymbol typeParam = mapTypeSymbol.typeParam();
            if (typeParam.getModule().isPresent()) {
                ModuleID id = typeParam.getModule().get().id();
                packageIdentifier = "%s:%s:%s".formatted(id.orgName(), id.moduleName(), id.version());
                moduleInfo = ModuleInfo.from(id);
            }
            type = CommonUtils.getTypeSignature(typeSymbol, moduleInfo);
        } else if (rawType instanceof TableTypeSymbol tableTypeSymbol) {
            kind = "TABLE_TYPE";
            TypeSymbol rowTypeParameter = tableTypeSymbol.rowTypeParameter();
            if (rowTypeParameter.getModule().isPresent()) {
                ModuleID id = rowTypeParameter.getModule().get().id();
                packageIdentifier = "%s:%s:%s".formatted(id.orgName(), id.moduleName(), id.version());
                moduleInfo = ModuleInfo.from(id);
            }
            type = CommonUtils.getTypeSignature(typeSymbol, moduleInfo);
        } else if (rawType instanceof StreamTypeSymbol) {
            kind = "STREAM_TYPE";
        } else if (rawType instanceof ObjectTypeSymbol) {
            kind = "OBJECT_TYPE";
        } else if (rawType instanceof FunctionTypeSymbol) {
            kind = "FUNCTION_TYPE";
        } else if (rawType instanceof ErrorTypeSymbol) {
            kind = "ERROR_TYPE";
        }

        String[] typeParts = type.split(":");
        if (typeParts.length > 1) {
            type = typeParts[1];
        }

        parameterData.typeMembers().add(new ParameterMemberTypeData(type, kind, packageIdentifier));
    }

    private Map<String, ParameterData> getIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                               boolean insert,
                                                               Map<String, String> documentationMap,
                                                               UnionTypeSymbol union) {
        Map<String, ParameterData> parameters = new LinkedHashMap<>();
        recordTypeSymbol.typeInclusions().forEach(includedType -> {
            if (includedType.getModule().isPresent() && includedType.getName().isPresent()) {
                ModuleID id = includedType.getModule().get().id();
                if (semanticModel != null) {
                    Optional<Symbol> typeByName = semanticModel.types().getTypeByName(id.orgName(), id.moduleName(),
                            "", includedType.getName().get());
                    if (typeByName.isPresent() && typeByName.get() instanceof TypeDefinitionSymbol typeDefSymbol) {
                        Optional<Documentation> documentation = typeDefSymbol.documentation();
                        documentation.ifPresent(doc -> documentationMap.putAll(doc.parameterMap()));
                    }
                }
            }
            parameters.putAll(getIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(includedType), insert,
                    documentationMap, union));
        });
        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol typeSymbol = recordFieldSymbol.typeDescriptor();
            TypeSymbol fieldType = CommonUtil.getRawType(typeSymbol);
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
            String paramDescription = getDescription(entry.getValue());
            if (documentationMap.containsKey(paramName) && !paramDescription.isEmpty()) {
                documentationMap.put(paramName, paramDescription);
            } else if (!documentationMap.containsKey(paramName)) {
                documentationMap.put(paramName, paramDescription);
            }
            if (!insert) {
                continue;
            }

            String defaultValue = getDefaultValue(recordFieldSymbol, fieldType);
            String paramType = getTypeSignature(typeSymbol);
            boolean optional = recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue();
            ParameterData parameterData = ParameterData.from(paramName, documentationMap.get(paramName),
                    getLabel(recordFieldSymbol.annotAttachments()),
                    paramType, defaultValue, ParameterData.Kind.INCLUDED_FIELD, optional,
                    getImportStatements(typeSymbol));
            parameters.put(paramName, parameterData);
            addParameterMemberTypes(typeSymbol, parameterData, union);
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = getTypeSignature(typeSymbol);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            parameters.put("Additional Values", ParameterData.from("Additional Values",
                    "Capture key value pairs", paramType, defaultValue,
                    ParameterData.Kind.INCLUDED_RECORD_REST, true,
                    getImportStatements(typeSymbol)));
        });
        return parameters;
    }

    private String getDefaultValue(Symbol paramSymbol, TypeSymbol typeSymbol) {
        String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);

        Optional<Location> symbolLocation = paramSymbol.getLocation();
        if (resolvedPackage == null || symbolLocation.isEmpty()) {
            return defaultValue;
        }

        Document document = findDocument(resolvedPackage, symbolLocation.get().lineRange().fileName());
        if (document == null) {
            return defaultValue;
        }

        ModulePartNode rootNode = document.syntaxTree().rootNode();
        TextRange textRange = symbolLocation.get().textRange();
        NonTerminalNode node = rootNode.findNode(TextRange.from(textRange.startOffset(), textRange.length()));

        ExpressionNode expression;
        switch (node.kind()) {
            case DEFAULTABLE_PARAM -> expression = (ExpressionNode) ((DefaultableParameterNode) node).expression();
            case RECORD_FIELD_WITH_DEFAULT_VALUE -> expression = ((RecordFieldWithDefaultValueNode) node).expression();
            default -> {
                return defaultValue;
            }
        }

        if (expression instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
            return resolvedPackage.packageName().value() + ":" + simpleNameReferenceNode.name().text();
        } else if (expression instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) {
            return qualifiedNameReferenceNode.modulePrefix().text() + ":" + qualifiedNameReferenceNode.identifier()
                    .text();
        } else {
            return expression.toSourceCode();
        }
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

    public static void allMembers(Map<String, TypeSymbol> typeMap, TypeSymbol typeSymbol) {

        switch (typeSymbol.typeKind()) {
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                unionTypeSymbol.memberTypeDescriptors().forEach(memberType -> allMembers(typeMap, memberType));
            }
            case INTERSECTION -> {
                IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
                intersectionTypeSymbol.memberTypeDescriptors().forEach(memberType -> allMembers(typeMap, memberType));
            }
            case STREAM -> {
                StreamTypeSymbol streamTypeSymbol = (StreamTypeSymbol) typeSymbol;
                allMembers(typeMap, streamTypeSymbol.typeParameter());
                allMembers(typeMap, streamTypeSymbol.completionValueTypeParameter());
            }
            case ARRAY -> {
                ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) typeSymbol;
                allMembers(typeMap, arrayTypeSymbol.memberTypeDescriptor());
            }
            case MAP -> {
                MapTypeSymbol mapTypeSymbol = (MapTypeSymbol) typeSymbol;
                allMembers(typeMap, mapTypeSymbol.typeParam());
            }
            case TABLE -> {
                TableTypeSymbol tableTypeSymbol = (TableTypeSymbol) typeSymbol;
                allMembers(typeMap, tableTypeSymbol.rowTypeParameter());
                tableTypeSymbol.keyConstraintTypeParameter().ifPresent(keyType -> allMembers(typeMap, keyType));
            }
            case RECORD -> {
                RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeSymbol;
                recordTypeSymbol.fieldDescriptors()
                        .forEach((key, value) -> allMembers(typeMap, value.typeDescriptor()));
                recordTypeSymbol.restTypeDescriptor().ifPresent(restType -> allMembers(typeMap, restType));
            }
            default -> typeMap.put(typeSymbol.getName().orElse(""), typeSymbol);
        }
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
                        String type =
                                CommonUtils.getTypeSignature(semanticModel, pathParameterSymbol.typeDescriptor(),
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

    private String getTypeSignature(String type) {
        if (userModuleInfo == null) {
            return moduleInfo.moduleName() + ":" + type;
        }
        return type;
    }

    private String getFunctionName() {
        // Get the client name if it is the init method of the client
        if (functionKind == FunctionData.Kind.CONNECTOR) {
            if (parentSymbolType != null) {
                return parentSymbolType;
            }
            if (parentSymbol != null) {
                return CommonUtils.getClassType(moduleInfo.packageName(),
                        parentSymbol.getName().orElse(functionName));
            }
        }
        return functionName;
    }

    private String getDescription(Documentable documentable) {
        return documentable.documentation().flatMap(Documentation::description).orElse("");
    }

    private String getImportStatements(TypeSymbol typeSymbol) {
        if (isCurrentModule && typeSymbol.getModule()
                .map(moduleSymbol -> ModuleInfo.from(moduleSymbol.id()).equals(userModuleInfo)).orElse(false)) {
            return null;
        }
        return CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null);
    }

    private void notifyClient(MessageType messageType, String message) {
        if (lsClientLogger != null) {
            String signature =
                    String.format("%s/%s:%s", moduleInfo.org(), moduleInfo.packageName(), moduleInfo.version());
            lsClientLogger.notifyClient(messageType, String.format(message, signature));
        }
    }

    private String getLabel(List<AnnotationAttachmentSymbol> annotationAttachmentSymbols) {
        for (AnnotationAttachmentSymbol annotAttachment : annotationAttachmentSymbols) {
            AnnotationSymbol annotationSymbol = annotAttachment.typeDescriptor();
            Optional<String> optName = annotationSymbol.getName();
            if (optName.isEmpty()) {
                continue;
            }
            if (!optName.get().equals(DISPLAY_ANNOTATION)) {
                continue;
            }
            Optional<ConstantValue> optAttachmentValue = annotAttachment.attachmentValue();
            if (optAttachmentValue.isEmpty()) {
                break;
            }
            ConstantValue attachmentValue = optAttachmentValue.get();
            if (attachmentValue.valueType().typeKind() != TypeDescKind.RECORD) {
                throw new IllegalStateException("Annotation attachment value is not a record");
            }
            HashMap<?, ?> valueMap = (HashMap<?, ?>) attachmentValue.value();
            Object label = valueMap.get(LABEL);
            if (label == null) {
                break;
            }
            return label.toString();
        }
        return "";
    }

    private boolean isAgentModelType(String paramName) {
        return moduleInfo.org().equals("ballerinax") && moduleInfo.moduleName().equals("ai")
                && (functionKind == FunctionData.Kind.CLASS_INIT || functionKind == FunctionData.Kind.CONNECTOR)
                && paramName.equals("modelType"); // TODO: Check Param Name
    }

    private record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }

    private record ReturnData(String returnType, ParamForTypeInfer paramForTypeInfer, boolean returnError,
                              String importStatements) {
    }
}
