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
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import org.ballerinalang.diagramutil.connector.models.connector.Type;

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

    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        DatabaseManager.createDatabase();
        System.setProperty("ballerina.home", "/Library/Ballerina/distributions/ballerina-2201.10.0");
        BuildProject buildProject = BuildProject.load(PROJECT_PATH);

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(PackageListGenerator.PACKAGE_JSON_PATH)) {
            Map<String, List<PackageListGenerator.PackageMetadataInfo>> packagesMap = gson.fromJson(reader, typeToken);
            List<PackageListGenerator.PackageMetadataInfo> ballerinaPackages = packagesMap.get("ballerina");
            resolvePackage(buildProject, "ballerina", ballerinaPackages.get(0));
        } catch (IOException e) {
            Logger.getGlobal().severe("Error reading packages JSON file: " + e.getMessage());
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
        Package resolvedPackage = resolutionResponse.get().resolvedPackage();
        PackageDescriptor descriptor = resolvedPackage.descriptor();
        int packageId = DatabaseManager.insertPackage(descriptor.org().value(), descriptor.name().value(),
                descriptor.version().value().toString());
        if (packageId == -1) {
            Logger.getGlobal().severe("Error inserting package to database: " + descriptor.name().value());
            return;
        }

        SemanticModel semanticModel;
        try {
            semanticModel = resolvedPackage.getDefaultModule().getCompilation().getSemanticModel();
        } catch (Exception e) {
            Logger.getGlobal().severe("Error reading semantic model: " + e.getMessage());
            return;
        }

        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() == SymbolKind.FUNCTION) {
                FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
                if (!functionSymbol.qualifiers().contains(Qualifier.PUBLIC)) {
                    continue;
                }

                // Capture the name of the function
                Optional<String> name = functionSymbol.getName();
                if (name.isEmpty()) {
                    continue;
                }

                // Obtain the description of the function
                Optional<Documentation> documentation = functionSymbol.documentation();
                String description = documentation.flatMap(Documentation::description).orElse("");
                Map<String, String> documentationMap = documentation.map(Documentation::parameterMap).orElse(Map.of());

                // Obtain the return type of the function
                FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
                Type returnType = functionTypeSymbol.returnTypeDescriptor().map(Type::fromSemanticSymbol).orElse(null);

                int functionId =
                        DatabaseManager.insertFunction(packageId, name.get(), description, gson.toJson(returnType));

                Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
                if (params.isEmpty()) {
                    continue;
                }
                for (ParameterSymbol paramSymbol : params.get()) {
                    String paramName = paramSymbol.getName().orElse("");
                    String paramType = gson.toJson(Type.fromSemanticSymbol(paramSymbol.typeDescriptor()));
                    String paramDescription = documentationMap.get(paramName);
                    ParameterKind parameterKind = paramSymbol.paramKind();
                    DatabaseManager.insertFunctionParameter(functionId, paramName, paramDescription, paramType,
                            parameterKind);
                }
                continue;
            }
            if (symbol.kind() == SymbolKind.CLASS) {
                ClassSymbol classSymbol = (ClassSymbol) symbol;
                if (!new HashSet<>(classSymbol.qualifiers()).containsAll(List.of(Qualifier.PUBLIC, Qualifier.CLIENT))) {
                    System.out.println(symbol.getName().get() + " is not a public connector");
                }
                System.out.println(symbol.getName().get());
                continue;
            }
        }
        List<Symbol> functions = semanticModel.moduleSymbols()
                .stream().filter(symbol -> symbol.kind() == SymbolKind.FUNCTION)
                .toList();
        return;
    }
}