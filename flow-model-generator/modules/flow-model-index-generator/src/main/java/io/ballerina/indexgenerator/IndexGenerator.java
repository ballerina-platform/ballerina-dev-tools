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
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.bala.BalaProject;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.repos.TempDirCompilationCache;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class IndexGenerator {

    private static final Path PROJECT_PATH =
            Path.of("/Users/nipunaf/projects/ballerina/ballerina-dev-tools/flow-model-generator/modules/flow-model" +
                    "-index-generator/src/main/resources/sample");

    private static final java.lang.reflect.Type typeToken =
            new TypeToken<Map<String, List<PackageListGenerator.PackageMetadataInfo>>>() { }.getType();

    private static final Logger LOGGER = Logger.getLogger(IndexGenerator.class.getName());

    public static void main(String[] args) {
        DatabaseManager.createDatabase();
        System.setProperty("ballerina.home", "/Library/Ballerina/distributions/ballerina-2201.10.0");
        BuildProject buildProject = BuildProject.load(PROJECT_PATH);

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(PackageListGenerator.PACKAGE_JSON_PATH)) {
            Map<String, List<PackageListGenerator.PackageMetadataInfo>> packagesMap = gson.fromJson(reader,
                    typeToken);
            packagesMap.forEach((key, value) -> value.forEach(
                    packageMetadataInfo -> resolvePackage(buildProject, key, packageMetadataInfo)));
        } catch (IOException e) {
            LOGGER.severe("Error reading packages JSON file: " + e.getMessage());
        }
    }

    private static void resolvePackage(BuildProject buildProject, String org,
                                       PackageListGenerator.PackageMetadataInfo packageMetadataInfo) {
        ResolutionRequest resolutionRequest = ResolutionRequest.from(
                PackageDescriptor.from(PackageOrg.from(org), PackageName.from(packageMetadataInfo.name()),
                        PackageVersion.from(packageMetadataInfo.version())));

        Collection<ResolutionResponse> resolutionResponses =
                buildProject.projectEnvironmentContext().getService(PackageResolver.class)
                        .resolvePackages(Collections.singletonList(resolutionRequest),
                                ResolutionOptions.builder().setOffline(false).build());
        Optional<ResolutionResponse> resolutionResponse = resolutionResponses.stream().findFirst();
        if (resolutionResponse.isEmpty()) {
            return;
        }

        Path balaPath = resolutionResponse.get().resolvedPackage().project().sourceRoot();
        ProjectEnvironmentBuilder defaultBuilder = ProjectEnvironmentBuilder.getDefaultBuilder();
        defaultBuilder.addCompilationCacheFactory(TempDirCompilationCache::from);
        BalaProject balaProject = BalaProject.loadProject(defaultBuilder, balaPath);
        Package resolvedPackage = balaProject.currentPackage();
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

        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() == SymbolKind.FUNCTION) {
                FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
                if (!functionSymbol.qualifiers().contains(Qualifier.PUBLIC)) {
                    continue;
                }

                processFunctionSymbol(functionSymbol, functionSymbol, packageId, FunctionType.FUNCTION,
                        descriptor.name().value());
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
                                descriptor.name().value());
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
                            descriptor.name().value());
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
                                             FunctionType functionType, String packageName) {
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
                .map(returnTypeDesc -> functionSymbol.nameEquals("init") ? getClientType(packageName, returnTypeDesc) :
                        getTypeSignature(returnTypeDesc)).orElse("");

        int functionId =
                DatabaseManager.insertFunction(packageId, name.get(), description, returnType,
                        functionType.name());

        // Handle the parameters of the function
        Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
        if (params.isEmpty()) {
            return packageId;
        }
        for (ParameterSymbol paramSymbol : params.get()) {
            String paramName = paramSymbol.getName().orElse("");
            String paramType = getTypeSignature(paramSymbol.typeDescriptor());
            String paramDescription = documentationMap.get(paramName);
            ParameterKind parameterKind = paramSymbol.paramKind();
            DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription, paramType,
                    parameterKind);
        }
        return functionId;
    }

    private static String getDescription(Documentable documentable) {
        return documentable.documentation().flatMap(Documentation::description).orElse("");
    }

    private static String getTypeSignature(TypeSymbol typeSymbol) {
        return switch (typeSymbol.typeKind()) {
            case TYPE_REFERENCE -> {
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                yield typeReferenceTypeSymbol.definition().getName()
                        .map(name -> typeReferenceTypeSymbol.getModule()
                                .flatMap(Symbol::getName)
                                .map(prefix -> prefix + ":" + name)
                                .orElse(name))
                        .orElseGet(() -> getTypeSignature(typeReferenceTypeSymbol.typeDescriptor()
                        ));
            }
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                yield unionTypeSymbol.memberTypeDescriptors().stream()
                        .map(IndexGenerator::getTypeSignature)
                        .reduce((s1, s2) -> s1 + "|" + s2)
                        .orElse(unionTypeSymbol.signature());
            }
            case INTERSECTION -> {
                IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
                yield intersectionTypeSymbol.memberTypeDescriptors().stream()
                        .map(IndexGenerator::getTypeSignature)
                        .reduce((s1, s2) -> s1 + " & " + s2)
                        .orElse(intersectionTypeSymbol.signature());
            }
            case TYPEDESC -> {
                TypeDescTypeSymbol typeDescTypeSymbol = (TypeDescTypeSymbol) typeSymbol;
                yield typeDescTypeSymbol.typeParameter()
                        .map(IndexGenerator::getTypeSignature)
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

    public static String getClientType(String importPrefix, TypeSymbol returnType) {
        String clientType = String.format("%s:%s", importPrefix, "Client");
        if (returnType.typeKind() != TypeDescKind.UNION) {
            return clientType;
        }

        UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) returnType;
        Optional<TypeSymbol> errorType = unionTypeSymbol.memberTypeDescriptors().stream()
                .filter(member -> member.typeKind() == TypeDescKind.ERROR)
                .findFirst();
        return errorType.map(type -> clientType + "|" + getTypeSignature(type)).orElse(clientType);
    }

    enum FunctionType {
        FUNCTION,
        REMOTE,
        CONNECTOR,
        RESOURCE
    }
}