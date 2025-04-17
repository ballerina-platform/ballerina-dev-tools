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

package io.ballerina.indexgenerator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.TypeBuilder;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.ErrorTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
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
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DefaultValueGeneratorUtil;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Index generator to cache functions and connectors.
 *
 * @since 2.0.0
 */
class ServiceIndexGenerator {

    private static final java.lang.reflect.Type typeToken = new TypeToken<Map<String,
            List<PackageMetadataInfo>>>() {
    }.getType();
    private static final Logger LOGGER = Logger.getLogger(ServiceIndexGenerator.class.getName());
    private static final String PACKAGE_JSON_FILE = "packages.json";

    public static void main(String[] args) {
        DatabaseManager.createDatabase();
        BuildProject buildProject = PackageUtil.getSampleProject();

        Gson gson = new Gson();
        URL resource = ServiceIndexGenerator.class.getClassLoader().getResource(PACKAGE_JSON_FILE);
        try (FileReader reader = new FileReader(Objects.requireNonNull(resource).getFile(), StandardCharsets.UTF_8)) {
            Map<String, List<PackageMetadataInfo>> packagesMap = gson.fromJson(reader,
                    typeToken);
            ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            forkJoinPool.submit(() -> packagesMap.forEach((key, value) -> value.forEach(
                    packageMetadataInfo -> resolvePackage(buildProject, key, packageMetadataInfo)))).join();
        } catch (IOException e) {
            LOGGER.severe("Error reading packages JSON file: " + e.getMessage());
        }
    }

    private static void resolvePackage(BuildProject buildProject, String org,
                                       PackageMetadataInfo packageMetadataInfo) {
        Package resolvedPackage;
        try {
            resolvedPackage = Objects.requireNonNull(PackageUtil.getModulePackage(buildProject, org,
                    packageMetadataInfo.name(), packageMetadataInfo.version())).orElseThrow();
        } catch (Throwable e) {
            LOGGER.severe("Error resolving package: " + packageMetadataInfo.name() + e.getMessage());
            return;
        }
        PackageDescriptor descriptor = resolvedPackage.descriptor();

        LOGGER.info("Processing package: " + descriptor.name().value());
        int packageId = DatabaseManager.insertPackage(descriptor.org().value(), descriptor.name().value(),
                descriptor.version().value().toString(), resolvedPackage.manifest().keywords());
        if (packageId == -1) {
            LOGGER.severe("Error inserting package to database: " + descriptor.name().value());
            return;
        }

        SemanticModel semanticModel;
        try {
            semanticModel = PackageUtil.getCompilation(resolvedPackage)
                    .getSemanticModel(resolvedPackage.getDefaultModule().moduleId());
        } catch (Exception e) {
            LOGGER.severe("Error reading semantic model: " + e.getMessage());
            return;
        }

        DatabaseManager.insertServiceDeclaration(packageId, packageMetadataInfo.serviceDeclaration());

        TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;

        Map<String, ServiceType> serviceTypes = packageMetadataInfo.serviceTypes();

        for (Symbol symbol : semanticModel.moduleSymbols()) {

            if (symbol.kind() == SymbolKind.CLASS) {
                ClassSymbol classSymbol = (ClassSymbol) symbol;

                if (classSymbol.nameEquals("Listener")) {

                    Optional<MethodSymbol> initMethodSymbol = classSymbol.initMethod();
                    if (initMethodSymbol.isEmpty()) {
                        continue;
                    }
                    processListenerInit(semanticModel, initMethodSymbol.get(), classSymbol, packageId,
                            errorTypeSymbol, resolvedPackage);
                }
                continue;
            }

            if (symbol instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                TypeSymbol typeSymbol = typeDefinitionSymbol.typeDescriptor();
                if (typeSymbol instanceof ObjectTypeSymbol objectTypeSymbol
                        && objectTypeSymbol.qualifiers().contains(Qualifier.SERVICE)) {
                    String serviceName = typeDefinitionSymbol.getName().orElse("Service");
                    if (!serviceTypes.containsKey(serviceName) && !packageMetadataInfo.serviceTypeSkipList()
                            .contains(serviceName)) {
                        ServiceType serviceType = new ServiceType(serviceName, getDescription(typeDefinitionSymbol),
                                null);
                        int serviceTypeId = DatabaseManager.insertServiceType(packageId, serviceType);
                        handleServiceType(objectTypeSymbol, semanticModel, serviceTypeId);
                    }
                }
            }

        }

        // insert hardcoded service types
        for (Map.Entry<String, ServiceType> entry : serviceTypes.entrySet()) {
            ServiceType serviceType = entry.getValue();
            int serviceTypeId =  DatabaseManager.insertServiceType(packageId, serviceType);
            for (ServiceTypeFunction function : serviceType.functions()) {
                int functionId = DatabaseManager.insertServiceTypeFunction(serviceTypeId, function);
                for (ServiceTypeFunctionParameter parameter : function.parameters()) {
                    DatabaseManager.insertServiceTypeFunctionParameter(functionId, parameter);
                }
            }
        }

        // hardcoded annotations
        if (Objects.nonNull(packageMetadataInfo.annotations())) {
            for (Map.Entry<String, Annotation> entry : packageMetadataInfo.annotations().entrySet()) {
                Annotation annotation = entry.getValue();
                String annotationName = entry.getKey();

                String attachPoints = String.join(",", annotation.attachmentPoints());
                String pkgInfo = "%s:%s:%s".formatted(descriptor.org().value(), descriptor.name().value(),
                        descriptor.version().value().toString());

                DatabaseManager.insertAnnotation(packageId, annotationName, attachPoints, annotation.displayName(),
                        annotation.description(), annotation.typeConstrain(), pkgInfo);
            }
        }
    }

    private static void processListenerInit(SemanticModel semanticModel, FunctionSymbol functionSymbol,
                                            Documentable documentable, int packageId,
                                            TypeSymbol errorTypeSymbol, Package resolvedPackage) {
        // Capture the name of the function
        Optional<String> name = functionSymbol.getName();
        if (name.isEmpty()) {
            return;
        }

        // Obtain the description of the function
        String description = getDescription(documentable);
        Map<String, String> documentationMap = functionSymbol.documentation().map(Documentation::parameterMap)
                .orElse(Map.of());

        // Obtain the return type of the function
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();

        int returnError = functionTypeSymbol.returnTypeDescriptor().map(returnTypeDesc ->
                CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol) ? 1 : 0).orElse(0);

        int functionId = DatabaseManager.insertListener(packageId, name.get(), description, returnError);

        ModuleInfo defaultModuleInfo = ModuleInfo.from(resolvedPackage.getDefaultModule().descriptor());
        functionTypeSymbol.params().ifPresent(
                paramList -> paramList.forEach(paramSymbol -> processParameterSymbol(paramSymbol,
                        documentationMap, functionId, resolvedPackage, null,
                        defaultModuleInfo, semanticModel)));
        functionTypeSymbol.restParam()
                .ifPresent(paramSymbol -> processParameterSymbol(paramSymbol,
                        documentationMap, functionId, resolvedPackage, null,
                        defaultModuleInfo, semanticModel));
    }

    private static void processParameterSymbol(ParameterSymbol paramSymbol, Map<String, String> documentationMap,
                                               int functionId, Package resolvedPackage,
                                               ParamForTypeInfer paramForTypeInfer,
                                               ModuleInfo defaultModuleInfo, SemanticModel semanticModel) {
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        FunctionParameterKind parameterKind = FunctionParameterKind.fromString(paramSymbol.paramKind().toString());
        String paramType;
        int optional = 1;
        String defaultValue;
        TypeSymbol typeSymbol = paramSymbol.typeDescriptor();
        String importStatements = CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null);
        if (parameterKind == FunctionParameterKind.REST_PARAMETER) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
            paramType = CommonUtils.getTypeSignature(semanticModel,
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor(), false);
        } else if (parameterKind == FunctionParameterKind.INCLUDED_RECORD) {
            Map<String, String> docMap = new HashMap<>();
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            if (typeSymbol.getModule().isPresent() && typeSymbol.getName().isPresent()) {
                ModuleID id = typeSymbol.getModule().get().id();
                Optional<Symbol> typeByName = semanticModel.types().getTypeByName(id.orgName(), id.moduleName(),
                        "", typeSymbol.getName().get());
                if (typeByName.isPresent() && typeByName.get() instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                    Optional<Documentation> documentation = typeDefinitionSymbol.documentation();
                    documentation.ifPresent(documentation1 -> docMap.putAll(documentation1.parameterMap()));
                }
            }
            addIncludedRecordParamsToDb((RecordTypeSymbol) CommonUtils.getRawType(typeSymbol),
                    functionId, resolvedPackage, defaultModuleInfo, semanticModel, true, docMap);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        } else if (parameterKind == FunctionParameterKind.REQUIRED) {
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            optional = 0;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    DatabaseManager.insertListenerParameter(functionId, paramName, paramDescription,
                            paramType, defaultValue, FunctionParameterKind.PARAM_FOR_TYPE_INFER, optional,
                            importStatements);
                    return;
                }
            }
            Location symbolLocation = paramSymbol.getLocation().get();
            Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            if (document != null) {
                defaultValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, resolvedPackage.packageName().value());
            }
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
        }
        int paramId = DatabaseManager.insertListenerParameter(functionId, paramName, paramDescription, paramType,
                defaultValue, parameterKind, optional, importStatements);
        insertParameterMemberTypes(paramId, typeSymbol, semanticModel);
    }

    protected static void addIncludedRecordParamsToDb(RecordTypeSymbol recordTypeSymbol, int functionId,
                                                      Package resolvedPackage, ModuleInfo defaultModuleInfo,
                                                      SemanticModel semanticModel, boolean insert,
                                                      Map<String, String> documentationMap) {
        recordTypeSymbol.typeInclusions().forEach(includedType -> {
            if (includedType.getModule().isPresent() && includedType.getName().isPresent()) {
                ModuleID id = includedType.getModule().get().id();
                Optional<Symbol> typeByName = semanticModel.types().getTypeByName(id.orgName(), id.moduleName(),
                        "", includedType.getName().get());
                if (typeByName.isPresent() && typeByName.get() instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                    Optional<Documentation> documentation = typeDefinitionSymbol.documentation();
                    documentation.ifPresent(documentation1 -> documentationMap.putAll(documentation1.parameterMap()));
                }
            }
            addIncludedRecordParamsToDb(((RecordTypeSymbol) CommonUtils.getRawType(includedType)), functionId,
                    resolvedPackage, defaultModuleInfo, semanticModel, false, documentationMap);
        });

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
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            int optional = 0;
            if (recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue()) {
                optional = 1;
            }
            int paramId = DatabaseManager.insertListenerParameter(functionId, paramName,
                    documentationMap.get(paramName), paramType, defaultValue,
                    FunctionParameterKind.INCLUDED_FIELD, optional,
                    CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null));
            insertParameterMemberTypes(paramId, typeSymbol, semanticModel);
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            DatabaseManager.insertListenerParameter(functionId, "Additional Values",
                    "Capture key value pairs", paramType, defaultValue,
                    FunctionParameterKind.INCLUDED_RECORD_REST, 1,
                    CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null));
        });
    }

    private static void insertParameterMemberTypes(int parameterId, TypeSymbol typeSymbol,
                                                   SemanticModel semanticModel) {
        Types types = semanticModel.types();
        TypeBuilder builder = semanticModel.types().builder();
        UnionTypeSymbol union = builder.UNION_TYPE.withMemberTypes(
                types.BOOLEAN, types.NIL, types.STRING, types.INT, types.FLOAT,
                types.DECIMAL, types.BYTE, types.REGEX, types.XML).build();

        if (typeSymbol instanceof UnionTypeSymbol unionTypeSymbol) {
            unionTypeSymbol.memberTypeDescriptors().forEach(
                    memberType -> insertParameterMemberTypes(parameterId, memberType, semanticModel));
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

        DatabaseManager.insertParameterMemberType(parameterId, type, kind, packageIdentifier);
    }

    private static String getDescription(Documentable documentable) {
        return documentable.documentation().flatMap(Documentation::description).orElse("");
    }

    private static void handleServiceType(ObjectTypeSymbol objectTypeSymbol, SemanticModel semanticModel,
                                          int serviceTypeId) {

        TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;

        objectTypeSymbol.methods().forEach((methodName, methodSymbol) -> {
            if (methodSymbol.qualifiers().contains(Qualifier.REMOTE)) {
                Optional<Documentation> documentation = methodSymbol.documentation();
                String methodDescription = "";
                Map<String, String> paramDocMap = new HashMap<>();
                if (documentation.isPresent()) {
                    methodDescription = documentation.get().description().orElse("");
                    paramDocMap = documentation.get().parameterMap();
                }

                TypeSymbol returnType = methodSymbol.typeDescriptor().returnTypeDescriptor().orElse(null);
                int returnError = returnType != null && CommonUtils.subTypeOf(returnType, errorTypeSymbol) ? 1 : 0;
                String returnTypeSignature = Objects.isNull(returnType) ? "" : CommonUtils.getTypeSignature(
                        semanticModel, returnType, false);


                List<ServiceTypeFunctionParameter> parameters = new ArrayList<>();
                ServiceTypeFunction function = new ServiceTypeFunction(methodName,
                        methodDescription, "", "REMOTE", returnTypeSignature,
                        0, returnError, "", 1, parameters);

                int functionId = DatabaseManager.insertServiceTypeFunction(serviceTypeId, function);

                FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();
                Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
                if (params.isPresent()) {
                    for (ParameterSymbol param : params.get()) {
                        String paramName = param.getName().orElse("param");
                        String paramDescription = paramDocMap.get(paramName) == null ? "" : paramDocMap.get(paramName);
                        ServiceTypeFunctionParameter parameter = new ServiceTypeFunctionParameter(
                                paramName, paramName, paramDescription, param.paramKind().name(),
                                CommonUtils.getTypeSignature(semanticModel, param.typeDescriptor(), false),
                                "", "", 0, 0
                        );
                        DatabaseManager.insertServiceTypeFunctionParameter(functionId, parameter);
                    }
                }
            } else if (methodSymbol.qualifiers().contains(Qualifier.RESOURCE)) {
                Optional<Documentation> documentation = methodSymbol.documentation();
                String methodDescription = "";
                Map<String, String> paramDocMap = new HashMap<>();
                if (documentation.isPresent()) {
                    methodDescription = documentation.get().description().orElse("");
                    paramDocMap = documentation.get().parameterMap();
                }

                ResourceMethodSymbol resourceMethodSymbol = (ResourceMethodSymbol) methodSymbol;
                String path = getPath(resourceMethodSymbol, semanticModel);

                List<ServiceTypeFunctionParameter> parameters = new ArrayList<>();

                TypeSymbol returnType = methodSymbol.typeDescriptor().returnTypeDescriptor().orElse(null);
                int returnError = returnType != null && CommonUtils.subTypeOf(returnType, errorTypeSymbol) ? 1 : 0;
                String returnTypeSignature = Objects.isNull(returnType) ? "" : CommonUtils.getTypeSignature(
                        semanticModel, returnType, false);

                ServiceTypeFunction function = new ServiceTypeFunction(path,
                        methodDescription, resourceMethodSymbol.getName().orElse("get"), "RESOURCE",
                        returnTypeSignature, 0, returnError, "", 1, parameters);

                int functionId = DatabaseManager.insertServiceTypeFunction(serviceTypeId, function);

                FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();
                Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
                if (params.isPresent()) {
                    for (ParameterSymbol param : params.get()) {
                        String paramName = param.getName().orElse("param");
                        String paramDescription = paramDocMap.get(paramName) == null ? "" : paramDocMap.get(paramName);
                        ServiceTypeFunctionParameter parameter = new ServiceTypeFunctionParameter(
                                paramName, paramName, paramDescription, param.paramKind().name(),
                                CommonUtils.getTypeSignature(semanticModel, param.typeDescriptor(), false),
                                "", "", 0, 0
                        );
                        DatabaseManager.insertServiceTypeFunctionParameter(functionId, parameter);
                    }
                }
            }
        });
    }

    enum FunctionParameterKind {
        REQUIRED,
        DEFAULTABLE,
        INCLUDED_RECORD,
        REST_PARAMETER,
        INCLUDED_FIELD,
        PARAM_FOR_TYPE_INFER,
        INCLUDED_RECORD_REST,
        PATH_PARAM,
        PATH_REST_PARAM;

        // need to have a fromString logic here
        public static FunctionParameterKind fromString(String value) {
            if (value.equals("REST")) {
                return REST_PARAMETER;
            }
            return FunctionParameterKind.valueOf(value);
        }
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

    public static Document findDocument(Package pkg, String path) {
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

    private record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }

    private record PackageMetadataInfo(String name, String version, List<String> serviceTypeSkipList,
                                       ServiceDeclaration serviceDeclaration,
                                       Map<String, ServiceType> serviceTypes, Map<String, Annotation> annotations) {
    }

    record ServiceDeclaration(int optionalTypeDescriptor, String displayName, String typeDescriptorLabel,
                              String typeDescriptorDescription, String typeDescriptorDefaultValue,
                              int addDefaultTypeDescriptor, int optionalAbsoluteResourcePath,
                              String absoluteResourcePathLabel, String absoluteResourcePathDescription,
                              String absoluteResourcePathDefaultValue, int optionalStringLiteral,
                              String stringLiteralLabel, String stringLiteralDescription,
                              String stringLiteralDefaultValue, String listenerKind, String kind) {
    }

    record Annotation(List<String> attachmentPoints, String displayName, String description, String typeConstrain) {
    }

    record ServiceType(
            String name,
            String description,
            List<ServiceTypeFunction> functions
    ) {
    }

    record ServiceTypeFunction(
            String name,
            String description,
            String accessor,
            String kind,
            String returnType,
            int returnTypeEditable,
            int returnError,
            String importStatements,
            int enable,
            List<ServiceTypeFunctionParameter> parameters
    ) {
    }

    record ServiceTypeFunctionParameter(
            String name,
            String label,
            String description,
            String kind,
            String type, // Store JSON as String
            String defaultValue,
            String importStatements,
            int nameEditable,
            int typeEditable
    ) {
    }

    private static String getPath(ResourceMethodSymbol resourceMethodSymbol, SemanticModel semanticModel) {
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        List<String> paths = new ArrayList<>();
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                for (Symbol pathSegment : pathSegmentList.list()) {
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        String type = CommonUtils.getTypeSignature(semanticModel, pathParameterSymbol.typeDescriptor(),
                                true);
                        String paramName = pathParameterSymbol.getName().orElse("");
                        paths.add("[%s %s]".formatted(type, paramName));
                    } else {
                        paths.add(pathSegment.getName().orElse(""));
                    }
                }
            }
            case DOT_RESOURCE_PATH -> paths.add(".");
            default -> paths.add("");
        }
        return String.join("/", paths);
    }
}
