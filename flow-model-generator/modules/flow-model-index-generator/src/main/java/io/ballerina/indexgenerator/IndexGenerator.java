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
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
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
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.DefaultValueGeneratorUtil;
import io.ballerina.flowmodelgenerator.core.utils.PackageUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
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
            new TypeToken<Map<String, List<PackageListGenerator.PackageMetadataInfo>>>() { }.getType();
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
            forkJoinPool.submit(() -> packagesMap.forEach((key, value) -> value.parallelStream().forEach(
                    packageMetadataInfo -> resolvePackage(buildProject, key, packageMetadataInfo)
            ))).join();
        } catch (IOException e) {
            LOGGER.severe("Error reading packages JSON file: " + e.getMessage());
        }
    }

    private static void resolvePackage(BuildProject buildProject, String org,
                                       PackageListGenerator.PackageMetadataInfo packageMetadataInfo) {
        Package resolvedPackage;
        try {
            resolvedPackage = Objects.requireNonNull(PackageUtil.getModulePackage(buildProject, org,
                    packageMetadataInfo.name(), packageMetadataInfo.version()));
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
            semanticModel =
                    resolvedPackage.getCompilation().getSemanticModel(resolvedPackage.getDefaultModule().moduleId());
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

                processFunctionSymbol(functionSymbol, functionSymbol, packageId, FunctionType.FUNCTION,
                        descriptor.name().value(), errorTypeSymbol, resolvedPackage);
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
                int connectorId =
                        processFunctionSymbol(initMethodSymbol.get(), classSymbol, packageId, FunctionType.CONNECTOR,
                                descriptor.name().value(), errorTypeSymbol, resolvedPackage);
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
                    int functionId = processFunctionSymbol(methodSymbol, methodSymbol, packageId, functionType,
                            descriptor.name().value(), errorTypeSymbol, resolvedPackage);
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

    private static int processFunctionSymbol(FunctionSymbol functionSymbol, Documentable documentable, int packageId,
                                             FunctionType functionType, String packageName,
                                             TypeSymbol errorTypeSymbol, Package resolvedPackage) {
        String pathBuilder = "";
        if (functionType == FunctionType.RESOURCE) {
            pathBuilder = ParamUtils.buildResourcePathTemplate(functionSymbol);
        }

        // Capture the name of the function
        Optional<String> name = functionSymbol.getName();
        if (name.isEmpty()) {
            return packageId;
        }

        // Obtain the description of the function
        String description = getDescription(documentable);
        Map<String, String> documentationMap =
                functionSymbol.documentation().map(Documentation::parameterMap).orElse(Map.of());

        // Obtain the return type of the function
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        String returnType = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> functionSymbol.nameEquals("init") ?
                        getClientType(packageName) :
                        getTypeSignature(returnTypeDesc, errorTypeSymbol, true)).orElse("");

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
                    String defaultValue = DefaultValueGeneratorUtil
                            .getDefaultValueForType(returnTypeMap.get(paramName));
                    paramForTypeInfer = new ParamForTypeInfer(paramName, defaultValue, returnType);
                    break;
                }
            }
        }

        int returnError = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol) ? 1 : 0).orElse(0);

        int functionId =
                DatabaseManager.insertFunction(packageId, name.get(), description, returnType,
                        functionType.name(), pathBuilder, returnError);

        // Handle the parameters of the function
        ParamForTypeInfer finalParamForTypeInfer = paramForTypeInfer;
        functionTypeSymbol.params().ifPresent(paramList -> paramList.forEach(paramSymbol ->
                processParameterSymbol(paramSymbol, documentationMap, functionId, resolvedPackage,
                        finalParamForTypeInfer)));
        functionTypeSymbol.restParam().ifPresent(paramSymbol ->
                processParameterSymbol(paramSymbol, documentationMap, functionId, resolvedPackage, null));
        return functionId;
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

    private static void processParameterSymbol(ParameterSymbol paramSymbol, Map<String, String> documentationMap,
                                               int functionId, Package resolvedPackage,
                                               ParamForTypeInfer paramForTypeInfer) {
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        FunctionParameterKind parameterKind = FunctionParameterKind.fromString(paramSymbol.paramKind().toString());
        String paramType;
        int optional = 1;
        String defaultValue;
        if (parameterKind == FunctionParameterKind.REST_PARAMETER) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) paramSymbol.typeDescriptor()).memberTypeDescriptor());
            paramType = getTypeSignature(((ArrayTypeSymbol) paramSymbol.typeDescriptor()).memberTypeDescriptor(),
                    null, false);
        } else if (parameterKind == FunctionParameterKind.INCLUDED_RECORD) {
            paramType = getTypeSignature(paramSymbol.typeDescriptor(), null, false);
            addIncludedRecordParamsToDb((RecordTypeSymbol) CommonUtils.getRawType(paramSymbol.typeDescriptor()),
                    functionId, resolvedPackage);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
        } else if (parameterKind == FunctionParameterKind.REQUIRED) {
            paramType = getTypeSignature(paramSymbol.typeDescriptor(), null, false);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            optional = 0;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription,
                            paramType, defaultValue, FunctionParameterKind.PARAM_FOR_TYPE_INFER, optional);
                    return;
                }
            }
            Location symbolLocation = paramSymbol.getLocation().get();
            Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            if (document != null) {
                defaultValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, resolvedPackage.packageName().value());
            }
            paramType = getTypeSignature(paramSymbol.typeDescriptor(), null, false);
        }
        DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription, paramType, defaultValue,
                parameterKind, optional);
    }

    protected static void addIncludedRecordParamsToDb(RecordTypeSymbol recordTypeSymbol, int functionId,
                                                      Package resolvedPackage) {
        recordTypeSymbol.typeInclusions().forEach(includedType -> {
            addIncludedRecordParamsToDb(((RecordTypeSymbol) CommonUtils.getRawType(includedType)), functionId,
                    resolvedPackage);
        });
        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol fieldType = CommonUtil.getRawType(recordFieldSymbol.typeDescriptor());
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
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
            String paramDescription = entry.getValue().documentation()
                    .flatMap(Documentation::description).orElse("");
            String paramType = getTypeSignature(recordFieldSymbol.typeDescriptor(), null, false);
            int optional = 0;
            if (recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue()) {
                optional = 1;
            }
            DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription, paramType, defaultValue,
                    FunctionParameterKind.INCLUDED_FIELD, optional);
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = getTypeSignature(typeSymbol, null, false);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            DatabaseManager.insertFunctionParameter(functionId, "Additional Values",
                    "Capture key value pairs", paramType, defaultValue,
                    FunctionParameterKind.INCLUDED_RECORD_REST, 1);
        });
    }

    private static String getDescription(Documentable documentable) {
        return documentable.documentation().flatMap(Documentation::description).orElse("");
    }

    private static String getTypeSignature(TypeSymbol typeSymbol, TypeSymbol errorTypeSymbol, boolean ignoreError) {
        return switch (typeSymbol.typeKind()) {
            case TYPE_REFERENCE -> {
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                yield typeReferenceTypeSymbol.definition().getName()
                        .map(name -> typeReferenceTypeSymbol.getModule()
                                .flatMap(Symbol::getName)
                                .map(prefix -> prefix + ":" + name)
                                .orElse(name))
                        .orElseGet(() -> getTypeSignature(
                                typeReferenceTypeSymbol.typeDescriptor(), typeSymbol, ignoreError));
            }
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                if (ignoreError) {
                    yield unionTypeSymbol.memberTypeDescriptors().stream()
                            .filter(typeSymbol1 -> !typeSymbol1.subtypeOf(errorTypeSymbol))
                            .map(typeSymbol1 -> getTypeSignature(typeSymbol1, errorTypeSymbol, true))
                            .reduce((s1, s2) -> s1 + "|" + s2)
                            .orElse(unionTypeSymbol.signature());
                }
                yield unionTypeSymbol.memberTypeDescriptors().stream()
                        .map(typeSymbol1 -> getTypeSignature(typeSymbol1, errorTypeSymbol, false))
                        .reduce((s1, s2) -> s1 + "|" + s2)
                        .orElse(unionTypeSymbol.signature());
            }
            case INTERSECTION -> {
                IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
                yield intersectionTypeSymbol.memberTypeDescriptors().stream()
                        .map(typeSymbol1 -> getTypeSignature(typeSymbol1, errorTypeSymbol, ignoreError))
                        .reduce((s1, s2) -> s1 + " & " + s2)
                        .orElse(intersectionTypeSymbol.signature());
            }
            case TYPEDESC -> {
                TypeDescTypeSymbol typeDescTypeSymbol = (TypeDescTypeSymbol) typeSymbol;
                yield typeDescTypeSymbol.typeParameter()
                        .map(typeSymbol1 -> getTypeSignature(typeSymbol1, errorTypeSymbol, ignoreError))
                        .orElse(typeDescTypeSymbol.signature());
            }
            case ERROR -> {
                Optional<String> moduleName = typeSymbol.getModule()
                        .map(module -> {
                            String prefix = module.id().modulePrefix();
                            return "annotations".equals(prefix) ? null : prefix;
                        });
                yield moduleName.map(s -> s + ":").orElse("") + typeSymbol.getName().orElse("error");
            }
            default -> {
                Optional<String> moduleName = typeSymbol.getModule().map(module -> module.id().modulePrefix());
                yield moduleName.map(s -> s + ":").orElse("") + typeSymbol.signature();
            }
        };
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
        INCLUDED_RECORD_REST;

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
}
