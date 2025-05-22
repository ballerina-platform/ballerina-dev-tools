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
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DefaultValueGeneratorUtil;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
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
import java.util.HashSet;
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
class IndexGenerator {

    private static final java.lang.reflect.Type typeToken =
            new TypeToken<Map<String, List<PackageListGenerator.PackageMetadataInfo>>>() {
            }.getType();
    private static final Logger LOGGER = Logger.getLogger(IndexGenerator.class.getName());

    public static void main(String[] args) {
        DatabaseManager.createDatabase();
        BuildProject buildProject = PackageUtil.getSampleProject();

        Gson gson = new Gson();
        URL resource = IndexGenerator.class.getClassLoader().getResource(PackageListGenerator.PACKAGE_JSON_FILE);
        try (FileReader reader = new FileReader(Objects.requireNonNull(resource).getFile(), StandardCharsets.UTF_8)) {
            Map<String, List<PackageListGenerator.PackageMetadataInfo>> packagesMap = gson.fromJson(reader,
                    typeToken);
            ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
            forkJoinPool.submit(() -> packagesMap.forEach((key, value) -> value.forEach(
                    packageMetadataInfo -> resolvePackage(buildProject, key, packageMetadataInfo)))).join();
        } catch (IOException e) {
            LOGGER.severe("Error reading packages JSON file: " + e.getMessage());
        }

        // TODO: Remove this once thw raw parameter property type is introduced
        DatabaseManager.executeQuery("UPDATE Parameter SET default_value = '``' WHERE type = 'sql:ParameterizedQuery'");
//
        // TODO: Remove this once the package index is introduced
        DatabaseManager.executeQuery("UPDATE Parameter SET type= 'anydata', default_value= 'anydata' \n" +
                "WHERE parameter_id IN (\n" +
                "    SELECT p.parameter_id\n" +
                "    FROM Parameter p\n" +
                "    INNER JOIN Function f ON f.function_id = p.function_id\n" +
                "    INNER JOIN Package pack ON pack.package_id = f.package_id\n" +
                "    WHERE pack.name = 'http' AND p.name = 'targetType'\n" +
                ");");

        // TODO: Need to improve how we handle lang lib functions
        DatabaseManager.updateTypeParameter("lang.array", "array:Type1", "(any|error)");
        DatabaseManager.updateTypeParameter("lang.array", "array:Type", "(any|error)");
        DatabaseManager.updateTypeParameter("lang.array", "array:AnydataType", "(anydata|error)");
        DatabaseManager.updateTypeParameter("lang.error", "error:DetailType", "error:Detail");
        DatabaseManager.updateTypeParameter("lang.map", "map:Type1", "map<any|error>");
        DatabaseManager.updateTypeParameter("lang.map", "map:Type", "map<any|error>");
        DatabaseManager.updateTypeParameter("lang.stream", "stream:Type1", "(any|error)");
        DatabaseManager.updateTypeParameter("lang.stream", "stream:Type", "(any|error)");
        DatabaseManager.updateTypeParameter("lang.stream", "stream:ErrorType", "error");
        DatabaseManager.updateTypeParameter("lang.stream", "stream:CompletionType", "error");
        DatabaseManager.updateTypeParameter("lang.xml", "xml:XmlType", "xml");
        DatabaseManager.updateTypeParameter("lang.xml", "xml:ItemType",
                "(xml:Element|xml:Comment|xml:ProcessingInstruction|xml:Text)");
        DatabaseManager.updateTypeParameter("lang.table", "table:MapType1", "map<any|error>");
        DatabaseManager.updateTypeParameter("lang.table", "table:MapType", "map<any|error>");
        DatabaseManager.updateTypeParameter("lang.table", "table:KeyType", "anydata");
        DatabaseManager.updateTypeParameter("lang.table", "table:Type", "(any|error)");
        DatabaseManager.updateTypeParameter("lang.value", "value:AnydataType", "anydata");
        DatabaseManager.updateTypeParameter("lang.value", "value:Type", "(any|error)");
    }

    private static void resolvePackage(BuildProject buildProject, String org,
                                       PackageListGenerator.PackageMetadataInfo packageMetadataInfo) {
        Package resolvedPackage;
        try {
            resolvedPackage = Objects.requireNonNull(PackageUtil.getModulePackage(buildProject, org,
                    packageMetadataInfo.name(), packageMetadataInfo.version())).orElseThrow();
        } catch (Throwable e) {
            LOGGER.severe("Error resolving package: " + packageMetadataInfo.name() + e.getMessage());
            return;
        }
        for (Module module: resolvedPackage.modules()) {
            processModule(resolvedPackage, module);
        }
    }

    private static void processModule(Package resolvedPackage, Module module) {
        ModuleDescriptor descriptor = module.descriptor();
        String moduleName = descriptor.name().toString();
        LOGGER.info("Processing package: " + moduleName);
        int packageId = DatabaseManager.insertPackage(descriptor.org().value(),
               module.packageInstance().packageName().value(), moduleName,
                descriptor.version().value().toString(), resolvedPackage.manifest().keywords());
        if (packageId == -1) {
            LOGGER.severe("Error inserting package to database: " + moduleName);
            return;
        }

        SemanticModel semanticModel;
        try {
            semanticModel = PackageUtil.getCompilation(resolvedPackage)
                    .getSemanticModel(module.moduleId());
        } catch (Exception e) {
            LOGGER.severe("Error reading semantic model: " + e.getMessage());
            return;
        }

        TypeSymbol errorTypeSymbol = semanticModel.types().ERROR;

        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() == SymbolKind.FUNCTION) {
                FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
                if (!functionSymbol.qualifiers().contains(Qualifier.PUBLIC)) {
                    continue;
                }

                processFunctionSymbol(semanticModel, functionSymbol, functionSymbol, packageId, FunctionType.FUNCTION,
                        moduleName, errorTypeSymbol, module);
                continue;
            }
            if (symbol.kind() == SymbolKind.CLASS) {
                ClassSymbol classSymbol = (ClassSymbol) symbol;
                if (hasAllQualifiers(classSymbol.qualifiers(), List.of(Qualifier.PUBLIC, Qualifier.CLIENT))) {
                    continue;
                }

                Optional<MethodSymbol> initMethodSymbol = classSymbol.initMethod();
                if (initMethodSymbol.isEmpty()) {
                    continue;
                }
                if (!classSymbol.nameEquals("Client")) {
                    continue;
                }
                int connectorId = processFunctionSymbol(semanticModel, initMethodSymbol.get(), classSymbol, packageId,
                        FunctionType.CONNECTOR,
                        moduleName, errorTypeSymbol, module);
                if (connectorId == -1) {
                    continue;
                }

                // Process the actions of the client
                Map<String, MethodSymbol> methods = classSymbol.methods();
                for (Map.Entry<String, MethodSymbol> entry : methods.entrySet()) {
                    MethodSymbol methodSymbol = entry.getValue();

                    List<Qualifier> qualifiers = methodSymbol.qualifiers();
                    FunctionType functionType;
                    if (qualifiers.contains(Qualifier.REMOTE)) {
                        functionType = FunctionType.REMOTE;
                    } else if (qualifiers.contains(Qualifier.RESOURCE)) {
                        functionType = FunctionType.RESOURCE;
                    } else if (qualifiers.contains(Qualifier.PUBLIC)) {
                        functionType = FunctionType.FUNCTION;
                    } else {
                        continue;
                    }
                    int functionId = processFunctionSymbol(semanticModel, methodSymbol, methodSymbol, packageId,
                            functionType, moduleName, errorTypeSymbol, module);
                    if (functionId == -1) {
                        continue;
                    }
                    DatabaseManager.mapConnectorAction(functionId, connectorId);
                }
            }
        }
    }

    private static boolean hasAllQualifiers(List<Qualifier> actualQualifiers, List<Qualifier> expectedQualifiers) {
        return !new HashSet<>(actualQualifiers).containsAll(expectedQualifiers);
    }

    private static int processFunctionSymbol(SemanticModel semanticModel, FunctionSymbol functionSymbol,
                                             Documentable documentable, int packageId,
                                             FunctionType functionType, String packageName,
                                             TypeSymbol errorTypeSymbol, Module module) {
        // Capture the name of the function
        Optional<String> name = functionSymbol.getName();
        if (name.isEmpty()) {
            return packageId;
        }
        if (documentable instanceof ClassSymbol && name.get().equals("init")) {
            name = Optional.of("Client");
        }

        // Obtain the description of the function
        String description = getDescription(documentable);
        Map<String, String> documentationMap = functionSymbol.documentation().map(Documentation::parameterMap)
                .orElse(Map.of());

        // Obtain the return type of the function
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        Optional<TypeSymbol> returnTypeSymbol = functionTypeSymbol.returnTypeDescriptor();
        String returnType = returnTypeSymbol
                .map(returnTypeDesc -> functionSymbol.nameEquals("init") ? getClientType(packageName)
                        : CommonUtils.getTypeSignature(semanticModel, returnTypeDesc, true))
                .orElse("");

        // Get import statements for the return type
        ModuleInfo defaultModuleInfo = ModuleInfo.from(module.descriptor());
        String importStatements = returnTypeSymbol.flatMap(
                typeSymbol -> CommonUtils.getImportStatements(returnTypeSymbol.get(), defaultModuleInfo)).orElse(null);

        ParamForTypeInfer paramForTypeInfer = null;
        if (functionSymbol.external()) {
            List<String> paramNameList = new ArrayList<>();
            functionTypeSymbol.params().ifPresent(paramList -> paramList
                    .stream()
                    .filter(paramSym -> paramSym.paramKind() == ParameterKind.DEFAULTABLE)
                    .forEach(paramSymbol -> paramNameList.add(paramSymbol.getName().orElse(""))));

            Map<String, TypeSymbol> returnTypeMap = new HashMap<>();
            returnTypeSymbol.ifPresent(typeSymbol -> FunctionDataBuilder.allMembers(returnTypeMap, typeSymbol));
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

        int returnError = returnTypeSymbol
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol) ? 1 : 0).orElse(0);
        ParamUtils.ResourcePathTemplate resourcePathTemplate = null;
        if (functionType == FunctionType.RESOURCE) {
            resourcePathTemplate = ParamUtils.buildResourcePathTemplate(semanticModel, functionSymbol,
                    errorTypeSymbol);
        }

        String resourcePath = resourcePathTemplate == null ? "" : resourcePathTemplate.resourcePathTemplate();
        int functionId = DatabaseManager.insertFunction(packageId, name.get(), description, returnType,
                functionType.name(), resourcePath, returnError, paramForTypeInfer != null, importStatements);

        // Store the resource path params
        if (resourcePathTemplate != null) {
            List<ParameterData> parameterResults = resourcePathTemplate.pathParams();
            for (ParameterData parameterData : parameterResults) {
                DatabaseManager.insertFunctionParameter(functionId, parameterData.name(),
                        parameterData.description(), parameterData.type(), parameterData.defaultValue(),
                        FunctionParameterKind.fromString(parameterData.kind().name()),
                        parameterData.optional() ? 1 : 0, null);
            }
        }

        // Handle the parameters of the function
        ParamForTypeInfer finalParamForTypeInfer = paramForTypeInfer;
        functionTypeSymbol.params()
                .ifPresent(paramList -> paramList.forEach(paramSymbol -> processParameterSymbol(paramSymbol,
                        documentationMap, functionId, module,
                        finalParamForTypeInfer, defaultModuleInfo, semanticModel)));
        functionTypeSymbol.restParam()
                .ifPresent(paramSymbol -> processParameterSymbol(paramSymbol, documentationMap, functionId,
                        module, null,
                        defaultModuleInfo, semanticModel));
        return functionId;
    }

    private static void processParameterSymbol(ParameterSymbol paramSymbol, Map<String, String> documentationMap,
                                               int functionId, Module module,
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
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            addIncludedRecordParamsToDb((RecordTypeSymbol) CommonUtils.getRawType(typeSymbol),
                    functionId, module, defaultModuleInfo, semanticModel, true, new HashMap<>());
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
                    DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription,
                            paramType, defaultValue, FunctionParameterKind.PARAM_FOR_TYPE_INFER, optional,
                            importStatements);
                    return;
                }
            }
            Location symbolLocation = paramSymbol.getLocation().get();
            Document document = findDocument(module, symbolLocation.lineRange().fileName());
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            if (document != null) {
                defaultValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, module.descriptor().packageName().value());
            }
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
        }
        int paramId = DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription, paramType,
                defaultValue, parameterKind, optional, importStatements);
        insertParameterMemberTypes(paramId, typeSymbol, semanticModel);
    }

    protected static void addIncludedRecordParamsToDb(RecordTypeSymbol recordTypeSymbol, int functionId,
                                                      Module module, ModuleInfo defaultModuleInfo,
                                                      SemanticModel semanticModel, boolean insert,
                                                      Map<String, String> documentationMap) {
        recordTypeSymbol.typeInclusions().forEach(includedType -> addIncludedRecordParamsToDb(
                ((RecordTypeSymbol) CommonUtils.getRawType(includedType)), functionId, module,
                defaultModuleInfo, semanticModel, false, documentationMap)
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

            Location symbolLocation = recordFieldSymbol.getLocation().get();
            Document document = findDocument(module, symbolLocation.lineRange().fileName());
            String defaultValue;
            if (document != null) {
                defaultValue = getAttributeDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, module.descriptor().packageName().value());
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
            int paramId = DatabaseManager.insertFunctionParameter(functionId, paramName,
                    documentationMap.get(paramName), paramType, defaultValue,
                    FunctionParameterKind.INCLUDED_FIELD, optional,
                    CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null));
            insertParameterMemberTypes(paramId, typeSymbol, semanticModel);
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            DatabaseManager.insertFunctionParameter(functionId, "Additional Values",
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

    public static String getClientType(String packageName) {
        String importPrefix = packageName.substring(packageName.lastIndexOf('.') + 1);
        return String.format("%s:%s", importPrefix, "Client");
    }

    enum FunctionType {
        FUNCTION,
        REMOTE,
        CONNECTOR,
        RESOURCE
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

        private FunctionParameterKind() {
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

    public static Document findDocument(Module module, String path) {
        Project project = module.project();
        Path docPath = module.isDefaultModule() ? project.sourceRoot().resolve(module.descriptor().packageName().value()).resolve(path)
                : project.sourceRoot().resolve("modules")
                .resolve(module.moduleName().moduleNamePart()).resolve(path);
        try {
            DocumentId documentId = project.documentId(docPath);
            return module.document(documentId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }
}
