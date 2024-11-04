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
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathRestParam;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.PackageUtil;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.directory.BuildProject;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

class IndexGenerator {

    private static final java.lang.reflect.Type typeToken =
            new TypeToken<Map<String, List<PackageListGenerator.PackageMetadataInfo>>>() { }.getType();

    private static final Logger LOGGER = Logger.getLogger(IndexGenerator.class.getName());
    private static final String TARGET_TYPE_NAME = "targetType";

    public static void main(String[] args) {
        DatabaseManager.createDatabase();
        // TODO: Set the distribution home √ia build.gradle
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
        Package resolvedPackage = PackageUtil.getModulePackage(buildProject, org, packageMetadataInfo.name(),
                packageMetadataInfo.version());
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
                        descriptor.name().value(), errorTypeSymbol);
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
                                descriptor.name().value(), errorTypeSymbol);
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
                            descriptor.name().value(), errorTypeSymbol);
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
                                             TypeSymbol errorTypeSymbol) {
        StringBuilder pathBuilder = new StringBuilder();
        if (functionType == FunctionType.RESOURCE) {
            ResourceMethodSymbol resourceMethodSymbol = (ResourceMethodSymbol) functionSymbol;
            ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
            switch (resourcePath.kind()) {
                case PATH_SEGMENT_LIST -> {
                    PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                    for (Symbol pathSegment : pathSegmentList.list()) {
                        pathBuilder.append("/");
                        if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                            String type = CommonUtil.getRawType(pathParameterSymbol.typeDescriptor())
                                    .signature();
                            pathBuilder.append("[").append(type).append("]");
                        } else {
                            pathBuilder.append(pathSegment.getName().orElse(""));
                        }
                    }
                    ((PathSegmentList) resourcePath).pathRestParameter().ifPresent(pathRestParameter -> {
                        String type = CommonUtil.getRawType(pathRestParameter.typeDescriptor())
                                .signature();
                        pathBuilder.append("[").append(type).append("...]");
                    });
                }
                case PATH_REST_PARAM -> {
                    String type = CommonUtil.getRawType(((PathRestParam) resourcePath).parameter()
                            .typeDescriptor()).signature();
                    pathBuilder.append("[").append(type).append("...]");
                }
                case DOT_RESOURCE_PATH -> {
                    pathBuilder.append(".");
                }
            }
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
                        getClientType(packageName, returnTypeDesc, errorTypeSymbol) :
                        getTypeSignature(returnTypeDesc, errorTypeSymbol, true)).orElse("");

        int returnError = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol) ? 1 : 0).orElse(0);

        int functionId =
                DatabaseManager.insertFunction(packageId, name.get(), description, returnType,
                        functionType.name(), pathBuilder.toString(), returnError);

        // Handle the parameters of the function
        functionTypeSymbol.params().ifPresent(paramList -> paramList.forEach(paramSymbol ->
                processParameterSymbol(paramSymbol, documentationMap, functionId)));
        functionTypeSymbol.restParam().ifPresent(paramSymbol ->
                processParameterSymbol(paramSymbol, documentationMap, functionId));
        return functionId;
    }

    private static void processParameterSymbol(ParameterSymbol paramSymbol, Map<String, String> documentationMap,
                                               int functionId) {
        String paramName = paramSymbol.getName().orElse("");
        String paramType = getTypeSignature(paramSymbol.typeDescriptor(),null, false);
        String paramDescription = documentationMap.get(paramName);
        ParameterKind parameterKind = paramSymbol.paramKind();
        DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription, paramType,
                parameterKind);
    }

    private static String getDescription(Documentable documentable) {
        return documentable.documentation().flatMap(Documentation::description).orElse("");
    }

    private static String getTypeSignature(TypeSymbol typeSymbol, TypeSymbol errorTypeSymbol, boolean ignoreError) {
        return switch (typeSymbol.typeKind()) {
            case TYPE_REFERENCE -> {
                // TODO: Improve the handling of dependable types.
                // Tracked with: https://github.com/wso2-enterprise/eggplant-project/issues/253
                if (typeSymbol.nameEquals(TARGET_TYPE_NAME)) {
                    yield "json";
                }
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

    public static String getClientType(String packageName, TypeSymbol returnType, TypeSymbol errorTypeSymbol) {
        String importPrefix = packageName.substring(packageName.lastIndexOf('.') + 1);
        return String.format("%s:%s", importPrefix, "Client");
    }

    enum FunctionType {
        FUNCTION,
        REMOTE,
        CONNECTOR,
        RESOURCE
    }
}
