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

package io.ballerina.indexgenerator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.ballerina.centralconnector.response.PackageResponse;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.Qualifiable;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.directory.BuildProject;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * A utility class that generates search indexes for Ballerina packages, types, functions, and connectors. This class
 * reads package metadata from a JSON file and processes each package to extract public symbols (functions, classes,
 * type definitions, and enums) along with their documentation. The extracted information is then stored in a search
 * database.
 *
 * @since 2.0.0
 */
public class SearchIndexGenerator {

    private static final java.lang.reflect.Type typeToken =
            new TypeToken<Map<String, List<SearchListGenerator.PackageMetadataInfo>>>() { }.getType();
    private static final Logger LOGGER = Logger.getLogger(SearchIndexGenerator.class.getName());
    private static final String CONNECTOR_EXCLUDE_JSON = "connector_exclude.json";

    public static void main(String[] args) {
        SearchDatabaseManager.createDatabase();
        BuildProject buildProject = PackageUtil.getSampleProject();

        Gson gson = new Gson();
        URL resource = IndexGenerator.class.getClassLoader().getResource(SearchListGenerator.PACKAGE_JSON_FILE);
        try (FileReader reader = new FileReader(Objects.requireNonNull(resource).getFile(), StandardCharsets.UTF_8)) {
            Map<String, List<SearchListGenerator.PackageMetadataInfo>> packagesMap = gson.fromJson(reader,
                    typeToken);
            int totalPackages = packagesMap.values().stream().mapToInt(List::size).sum();
            SearchIndexLogger logger = new SearchIndexLogger(totalPackages);
            packagesMap.forEach((key, packages) ->
                    packages.parallelStream().forEach(packageMetadataInfo -> {
                        try {
                            // Create separate thread with timeout for each package
                            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                try {
                                    resolvePackage(buildProject, key, packageMetadataInfo, logger);
                                } catch (Throwable e) {
                                    logger.error("Error processing package: " + packageMetadataInfo.name() + " " +
                                            e.getMessage());
                                }
                            });

                            // Apply 5-minute timeout
                            future.get(3, TimeUnit.MINUTES);
                        } catch (InterruptedException | ExecutionException e) {
                            logger.error(
                                    "Error processing package: " + packageMetadataInfo.name() + " " + e.getMessage());
                        } catch (TimeoutException e) {
                            logger.error("Processing timeout for package: " + packageMetadataInfo.name());
                        }
                    })
            );
        } catch (IOException e) {
            LOGGER.severe("Error reading packages JSON file: " + e.getMessage());
        }

        // Delete irrelevant connectors from the index
        try {
            resource = SearchIndexGenerator.class.getClassLoader().getResource(CONNECTOR_EXCLUDE_JSON);
            if (resource != null) {
                String jsonContent = Files.readString(Path.of(resource.toURI()), StandardCharsets.UTF_8);
                Map<String, List<String>> excludeMap =
                        gson.fromJson(jsonContent, new TypeToken<Map<String, List<String>>>() { }.getType());
                excludeMap.forEach(SearchDatabaseManager::deleteConnector);
            }
        } catch (URISyntaxException | IOException e) {
            LOGGER.severe("Error reading connector_exclude.json file: " + e.getMessage());
        }
    }

    private static void resolvePackage(BuildProject buildProject, String org,
                                       SearchListGenerator.PackageMetadataInfo packageMetadataInfo,
                                       SearchIndexLogger logger) throws Exception {
        Package resolvedPackage;
        try {
            resolvedPackage = Objects.requireNonNull(PackageUtil.getModulePackage(buildProject, org,
                    packageMetadataInfo.name(), packageMetadataInfo.version())).orElseThrow();
        } catch (Throwable e) {
            logger.error("Error resolving package: " + packageMetadataInfo.name() + " " + e.getMessage());
            return;
        }

        List<String> exportedModules = resolvedPackage.manifest().exportedModules();
        for (Module module: resolvedPackage.modules()) {
            if (exportedModules.contains(module.descriptor().name().toString())) {
                processModule(packageMetadataInfo, resolvedPackage, module);
            }
        }
        logger.completion(packageMetadataInfo.name());
    }

    private static void processModule(SearchListGenerator.PackageMetadataInfo packageMetadataInfo,
                                      Package resolvedPackage, Module module) throws Exception {
        ModuleDescriptor descriptor = module.descriptor();

        String moduleName = module.moduleName().toString();
        int packageId = SearchDatabaseManager.insertPackage(descriptor.org().value(), moduleName,
                module.packageInstance().packageName().value(), descriptor.version().value().toString(),
                packageMetadataInfo.pullCount(), resolvedPackage.manifest().keywords());

        if (packageId == -1) {
            throw new Exception("Error inserting package to database: " + module);
        }

        SemanticModel semanticModel = PackageUtil.getCompilation(resolvedPackage)
                .getSemanticModel(module.moduleId());

        for (Symbol symbol : semanticModel.moduleSymbols()) {
            switch (symbol.kind()) {
                case FUNCTION -> {
                    Optional<Info> info = getName(symbol);
                    if (info.isEmpty()) {
                        continue;
                    }
                    SearchDatabaseManager.insertFunction(info.get().name(), info.get().description(), packageId);
                }
                case CLASS -> {
                    ClassSymbol classSymbol = (ClassSymbol) symbol;
                    Optional<Info> info = getName(classSymbol);
                    if (info.isEmpty()) {
                        continue;
                    }

                    if (classSymbol.qualifiers().contains(Qualifier.CLIENT)) {
                        SearchDatabaseManager.insertConnector(info.get().name(), info.get().description(), "Connector",
                                packageId);
                    } else {
                        SearchDatabaseManager.insertType(info.get().name(), info.get().description(), "class",
                                packageId);
                    }
                }
                case TYPE_DEFINITION -> {
                    TypeDefinitionSymbol typeDefinitionSymbol = (TypeDefinitionSymbol) symbol;
                    Optional<Info> info = getName(typeDefinitionSymbol);
                    if (info.isEmpty()) {
                        continue;
                    }
                    String kind = CommonUtils.getRawType(typeDefinitionSymbol.typeDescriptor()).typeKind().getName();
                    SearchDatabaseManager.insertType(info.get().name(), info.get().description(), kind, packageId);
                }
                case ENUM -> {
                    Optional<Info> info = getName(symbol);
                    if (info.isEmpty()) {
                        continue;
                    }
                    SearchDatabaseManager.insertType(info.get().name(), info.get().description(), "enum", packageId);
                }
                default -> {
                    // Do nothing
                }
            }
        }
    }

    private static Optional<Info> getName(Symbol symbol) {
        if (!(symbol instanceof Qualifiable qualifiable) || !qualifiable.qualifiers().contains(Qualifier.PUBLIC)) {
            return Optional.empty();
        }
        Optional<String> name = symbol.getName();
        if (name.isEmpty()) {
            return Optional.empty();
        }
        if (!(symbol instanceof Documentable documentable)) {
            return Optional.empty();
        }
        String description = documentable.documentation().flatMap(Documentation::description).orElse("");
        return Optional.of(new Info(name.get(), description));
    }

    private record Info(String name, String description) { }

}
