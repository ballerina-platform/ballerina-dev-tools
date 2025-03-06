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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.flowmodelgenerator.core.TypesManager;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.TypeData;
import io.ballerina.flowmodelgenerator.core.type.RecordValueAnalyzer;
import io.ballerina.flowmodelgenerator.core.type.RecordValueGenerator;
import io.ballerina.flowmodelgenerator.extension.request.FilePathRequest;
import io.ballerina.flowmodelgenerator.extension.request.GetTypeRequest;
import io.ballerina.flowmodelgenerator.extension.request.RecordConfigRequest;
import io.ballerina.flowmodelgenerator.extension.request.RecordValueGenerateRequest;
import io.ballerina.flowmodelgenerator.extension.request.TypeUpdateRequest;
import io.ballerina.flowmodelgenerator.extension.request.UpdatedRecordConfigRequest;
import io.ballerina.flowmodelgenerator.extension.response.RecordConfigResponse;
import io.ballerina.flowmodelgenerator.extension.response.RecordValueGenerateResponse;
import io.ballerina.flowmodelgenerator.extension.response.TypeListResponse;
import io.ballerina.flowmodelgenerator.extension.response.TypeResponse;
import io.ballerina.flowmodelgenerator.extension.response.TypeUpdateResponse;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.diagramutil.connector.models.connector.Type;
import org.ballerinalang.diagramutil.connector.models.connector.types.RecordType;
import org.ballerinalang.diagramutil.connector.models.connector.types.UnionType;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("typesManager")
public class TypesManagerService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<TypeListResponse> getTypes(FilePathRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            TypeListResponse response = new TypeListResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return response;
                }
                TypesManager typesManager = new TypesManager(document.get());
                JsonElement allTypes = typesManager.getAllTypes();
                response.setTypes(allTypes);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<TypeResponse> getType(GetTypeRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            TypeResponse response = new TypeResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return response;
                }
                TypesManager typesManager = new TypesManager(document.get());
                JsonElement result = typesManager.getType(document.get(), request.linePosition());
                response.setType(result.getAsJsonObject().get("type").getAsJsonObject());
                response.setRefs(result.getAsJsonObject().get("refs").getAsJsonArray());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<TypeResponse> getGraphqlType(GetTypeRequest request) {
        // TODO: Different implementation may be needed with future requirements
        return getType(request);
    }

    @JsonRequest
    public CompletableFuture<TypeUpdateResponse> createGraphqlClassType(TypeUpdateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            TypeUpdateResponse response = new TypeUpdateResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                TypeData typeData = (new Gson()).fromJson(request.type(), TypeData.class);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return response;
                }
                TypesManager typesManager = new TypesManager(document.get());
                response.setName(typeData.name());
                response.setTextEdits(typesManager.createGraphqlClassType(filePath, typeData));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<TypeUpdateResponse> updateType(TypeUpdateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            TypeUpdateResponse response = new TypeUpdateResponse();
            try {
                Path filePath = Path.of(request.filePath());
                this.workspaceManager.loadProject(filePath);
                TypeData typeData = (new Gson()).fromJson(request.type(), TypeData.class);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return response;
                }
                TypesManager typesManager = new TypesManager(document.get());
                response.setName(typeData.name());
                response.setTextEdits(typesManager.updateType(filePath, typeData));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<RecordConfigResponse> recordConfig(RecordConfigRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            RecordConfigResponse response = new RecordConfigResponse();
            try {
                Codedata codedata = request.codedata();
                String orgName = codedata.org();
                String packageName = codedata.module();
                String versionName = codedata.version();
                Path filePath = Path.of(request.filePath());

                // Find the semantic model
                Optional<SemanticModel> semanticModel = PackageUtil.getSemanticModelIfMatched(workspaceManager,
                        filePath, orgName, packageName, versionName);
                if (semanticModel.isEmpty()) {
                    semanticModel = PackageUtil.getSemanticModel(orgName, packageName, versionName);
                }
                if (semanticModel.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("Package '%s/%s:%s' not found", orgName, packageName, versionName));
                }

                // Get the type symbol
                Optional<TypeSymbol> typeSymbol = semanticModel.get().moduleSymbols().parallelStream()
                        .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION &&
                                symbol.nameEquals(request.typeConstraint()))
                        .map(symbol -> ((TypeDefinitionSymbol) symbol).typeDescriptor())
                        .findFirst();
                if (typeSymbol.isEmpty()) {
                    throw new IllegalArgumentException(String.format("Type '%s' not found in package '%s/%s:%s'",
                            request.typeConstraint(),
                            orgName,
                            packageName,
                            versionName));
                }
                if (typeSymbol.get().typeKind() != TypeDescKind.RECORD) {
                    throw new IllegalArgumentException(
                            String.format("Type '%s' is not a record", request.typeConstraint()));
                }
                response.setRecordConfig(Type.fromSemanticSymbol(typeSymbol.get()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<RecordConfigResponse> updateRecordConfig(UpdatedRecordConfigRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            RecordConfigResponse response = new RecordConfigResponse();
            try {
                Codedata codedata = request.codedata();
                String orgName = codedata.org();
                String packageName = codedata.module();
                String versionName = codedata.version();
                Path filePath = Path.of(request.filePath());

                // Find the semantic model
                Optional<SemanticModel> semanticModel = PackageUtil.getSemanticModelIfMatched(workspaceManager,
                        filePath, orgName, packageName, versionName);
                if (semanticModel.isEmpty()) {
                    semanticModel = PackageUtil.getSemanticModel(orgName, packageName, versionName);
                }
                if (semanticModel.isEmpty()) {
                    throw new IllegalArgumentException(
                            String.format("Package '%s/%s:%s' not found", orgName, packageName, versionName));
                }

                // Get the type symbol
                Optional<TypeSymbol> typeSymbol = semanticModel.get().moduleSymbols().parallelStream()
                        .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION &&
                                symbol.nameEquals(request.typeConstraint()))
                        .map(symbol -> ((TypeDefinitionSymbol) symbol).typeDescriptor())
                        .findFirst();

                if (typeSymbol.isEmpty()) {
                    throw new IllegalArgumentException(String.format("Type '%s' not found in package '%s/%s:%s'",
                            request.typeConstraint(),
                            orgName,
                            packageName,
                            versionName));
                }

                ExpressionNode expressionNode = NodeParser.parseExpression(request.expr());
                Type type = Type.fromSemanticSymbol(typeSymbol.get());
                if (expressionNode instanceof MappingConstructorExpressionNode mapping) {
                    if (type instanceof RecordType recordType) {
                        RecordValueAnalyzer.updateTypeConfig(recordType, mapping);
                    } else if (type instanceof UnionType unionType) {
                        RecordValueAnalyzer.updateUnionTypeConfig(unionType, mapping);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid expression");
                }
                response.setRecordConfig(type);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }

    @JsonRequest
    public CompletableFuture<RecordConfigResponse> dumb(Request request) {
        return CompletableFuture.supplyAsync(() -> {
            RecordConfigResponse response = new RecordConfigResponse();
            try {
                // Process each detail in parallel, find the first selected type
                request.details().parallelStream()
                        .flatMap(detail -> {
                            try {
                                Type type = getType(
                                        detail.codedata(),
                                        request.filePath(),
                                        detail.typeConstraint(),
                                        request.expr()
                                );
                                return type.selected ? Stream.of(type) : Stream.empty();
                            } catch (Throwable e) {
                                return Stream.empty();
                            }
                        })
                        .findAny()
                        .ifPresent(response::setRecordConfig);
            } catch (Throwable e) {
                response.setError(e);
            }
            semanticModelCache.clear();
            return response;
        });
    }

    // Cache key for SemanticModel
    private record CacheKey(String org, String packageName, String version) {}

    // Cache for SemanticModel instances
    private static final ConcurrentHashMap<CacheKey, SemanticModel> semanticModelCache = new ConcurrentHashMap<>();

    private Optional<SemanticModel> getCachedSemanticModel(String org, String packageName, String version,
                                                           Path filePath) {
        // Check cache with filePath
        CacheKey keyWithPath = new CacheKey(org, packageName, version);
        SemanticModel cachedModel = semanticModelCache.get(keyWithPath);
        if (cachedModel != null) {
            return Optional.of(cachedModel);
        }

        // Try to load via filePath-specific method
        Optional<SemanticModel> model = PackageUtil.getSemanticModelIfMatched(
                workspaceManager, filePath, org, packageName, version
        );
        if (model.isPresent()) {
            semanticModelCache.put(keyWithPath, model.get());
            return model;
        }

        // Fallback to general package lookup
        CacheKey keyWithoutPath = new CacheKey(org, packageName, version);
        cachedModel = semanticModelCache.get(keyWithoutPath);
        if (cachedModel != null) {
            return Optional.of(cachedModel);
        }

        model = PackageUtil.getSemanticModel(org, packageName, version);
        model.ifPresent(m -> semanticModelCache.put(keyWithoutPath, m));
        return model;
    }

    private Type getType(CData codedata, String path, String typeConstraint, String expr) {
        String orgName = codedata.org();
        String packageName = codedata.module();
        String versionName = codedata.version();
        Path filePath = Path.of(path);

        // Retrieve cached or load new semantic model
        Optional<SemanticModel> semanticModel = getCachedSemanticModel(orgName, packageName, versionName, filePath);
        if (semanticModel.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Package '%s/%s:%s' not found", orgName, packageName, versionName)
            );
        }

        // Existing type resolution logic
        Optional<TypeSymbol> typeSymbol = semanticModel.get().moduleSymbols().parallelStream()
                .filter(symbol -> symbol.kind() == SymbolKind.TYPE_DEFINITION && symbol.nameEquals(typeConstraint))
                .map(symbol -> ((TypeDefinitionSymbol) symbol).typeDescriptor())
                .findFirst();

        if (typeSymbol.isEmpty()) {
            throw new IllegalArgumentException(String.format("Type '%s' not found in package '%s/%s:%s'",
                    typeConstraint, orgName, packageName, versionName));
        }

        // Rest of your existing type processing logic
        ExpressionNode expressionNode = NodeParser.parseExpression(expr);
        Type type = Type.fromSemanticSymbol(typeSymbol.get());

        if (expressionNode instanceof MappingConstructorExpressionNode mapping) {
            if (type instanceof RecordType recordType) {
                RecordValueAnalyzer.updateTypeConfig(recordType, mapping);
            } else if (type instanceof UnionType unionType) {
                RecordValueAnalyzer.updateUnionTypeConfig(unionType, mapping);
            }
        } else {
            throw new IllegalArgumentException("Invalid expression");
        }

        return type;
    }

    record Request(String filePath, String expr, List<TypeConstrainDetails> details) {
    }

    record CData(String org, String module, String version) {}

    record TypeConstrainDetails(CData codedata, String typeConstraint) {}

    @JsonRequest
    public CompletableFuture<RecordValueGenerateResponse> generateValue(RecordValueGenerateRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            RecordValueGenerateResponse response = new RecordValueGenerateResponse();
            try {
                response.setRecordValue(RecordValueGenerator.generate(request.type().getAsJsonObject()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }
}
