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

package io.ballerina.flowmodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.TypeHandler;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.openapi.core.generators.common.model.Filter;
import io.ballerina.openapi.core.generators.common.model.GenSrcFile;
import io.ballerina.openapi.core.generators.service.ServiceGenerationHandler;
import io.ballerina.openapi.core.generators.service.model.OASServiceMetadata;
import io.ballerina.openapi.core.generators.type.GeneratorConstants;
import io.ballerina.projects.*;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.swagger.v3.oas.models.OpenAPI;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.ballerina.openapi.core.generators.common.GeneratorConstants.DEFAULT_FILE_HEADER;

/**
 * Generates service from the OpenAPI contract.
 *
 * @since 1.4.0
 */
public class OpenApiServiceGenerator {

    private final WorkspaceManager workspaceManager;
    private final Path oAContractPath;
    private final Path projectPath;
    private final int port;
    public static final List<String> SUPPORTED_OPENAPI_VERSIONS = List.of("2.0", "3.0.0", "3.0.1", "3.0.2", "3.0.3",
            "3.1.0");
    public static final String LS = System.lineSeparator();
    public static final String CLOSE_BRACE = "}";
    public static final String IMPORT = "import ballerina/http;";
    public static final String SERVICE_DECLARATION = "service OASServiceType on new http:Listener(%s) {";
    public static final String SERVICE_OBJ_FILE = "service_contract.bal";
    public static final String SERVICE_IMPL_FILE = "service_implementation.bal";

    public OpenApiServiceGenerator(Path oAContractPath, Path projectPath, int port, WorkspaceManager workspaceManager) {
        this.oAContractPath = oAContractPath;
        this.projectPath = projectPath;
        this.workspaceManager = workspaceManager;
        this.port = port;
    }

    public LineRange generateService() throws IOException, BallerinaOpenApiException, FormatterException,
            WorkspaceDocumentException, EventSyncException {
        Filter filter = new Filter(new ArrayList<>(), new ArrayList<>());

        List<Diagnostic> diagnostics = new ArrayList<>();
        List<GenSrcFile> genFiles = generateBallerinaService(oAContractPath, filter, diagnostics);
        if (genFiles.isEmpty()) {
            throw new BallerinaOpenApiException("Cannot generate service from the given OpenAPI contract.");
        }

        List<String> errorMessages = new ArrayList<>();
        for (Diagnostic diagnostic : diagnostics) {
            DiagnosticSeverity severity = diagnostic.diagnosticInfo().severity();
            if (severity == DiagnosticSeverity.ERROR) {
                errorMessages.add(diagnostic.message());
            }
        }

        if (!errorMessages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String errorMessage : errorMessages) {
                sb.append(DiagnosticSeverity.ERROR).append(": ").append(errorMessage).append(System.lineSeparator());
            }
            throw new BallerinaOpenApiException(sb.toString());
        }

        writeGeneratedSources(genFiles, projectPath);

        Path serviceImplPath = projectPath.resolve(SERVICE_IMPL_FILE);
        GeneratedFiles generatedFileDetails = getFileDetails(genFiles);
        String serviceImplContent = genServiceDeclaration(generatedFileDetails, serviceImplPath);

        Project project = this.workspaceManager.loadProject(serviceImplPath);
        Package currentPackage = project.currentPackage();
        Module oldModule = currentPackage.module(ModuleName.from(currentPackage.packageName()));
        DocumentId serviceImplDocId = DocumentId.create(serviceImplPath.toString(), oldModule.moduleId());
        DocumentConfig documentConfig = DocumentConfig.from(
                serviceImplDocId, serviceImplContent, serviceImplPath.getFileName().toString());
        Module newModule = oldModule.modify().addDocument(documentConfig).apply();
        Document serviceImplDoc = newModule.document(serviceImplDocId);

        return LineRange.from(SERVICE_IMPL_FILE, LinePosition.from(1, 0),
                serviceImplDoc.syntaxTree().rootNode().lineRange().endLine());
    }

    public List<GenSrcFile> generateBallerinaService(Path openAPI, Filter filter, List<Diagnostic> diagnostics)
            throws IOException, FormatterException, BallerinaOpenApiException {
        OpenAPI openAPIDef = GeneratorUtils.normalizeOpenAPI(openAPI, false, false);
        if (openAPIDef.getInfo() == null) {
            throw new BallerinaOpenApiException("Info section of the definition file cannot be empty/null: " +
                    openAPI);
        }

        checkOpenAPIVersion(openAPIDef);

        // Validate the service generation
        List<String> complexPaths = GeneratorUtils.getComplexPaths(openAPIDef);
        if (!complexPaths.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("service generation can not be done as the openapi definition contain following complex " +
                    "path(s) :").append(System.lineSeparator());
            for (String path : complexPaths) {
                sb.append(path).append(System.lineSeparator());
            }
            throw new BallerinaOpenApiException(sb.toString());
        }

        OASServiceMetadata oasServiceMetadata = new OASServiceMetadata.Builder()
                .withOpenAPI(openAPIDef)
                .withFilters(filter)
                .withNullable(true)
                .withGenerateServiceType(false)
                .withGenerateServiceContract(true)
                .withGenerateWithoutDataBinding(false)
                .build();
        TypeHandler.createInstance(openAPIDef, true);
        ServiceGenerationHandler serviceGenerationHandler = new ServiceGenerationHandler();
        List<GenSrcFile> sourceFiles = generateFilesForService(serviceGenerationHandler, oasServiceMetadata);

        diagnostics.addAll(serviceGenerationHandler.getDiagnostics());
        diagnostics.addAll(TypeHandler.getInstance().getDiagnostics());
        return sourceFiles;
    }

    private List<GenSrcFile> generateFilesForService(ServiceGenerationHandler serviceGenerationHandler,
                                                     OASServiceMetadata oasServiceMetadata) throws
            FormatterException, BallerinaOpenApiException {
        List<GenSrcFile> sourceFiles = serviceGenerationHandler.generateServiceFiles(oasServiceMetadata);
        String schemaSyntaxTree = Formatter.format(TypeHandler.getInstance()
                .generateTypeSyntaxTree()).toSourceCode();
        if (!schemaSyntaxTree.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.MODEL_SRC, oasServiceMetadata.getSrcPackage(),
                    GeneratorConstants.TYPE_FILE_NAME,
                    (oasServiceMetadata.getLicenseHeader().isBlank() ? DEFAULT_FILE_HEADER :
                            oasServiceMetadata.getLicenseHeader()) + schemaSyntaxTree));
        }
        return sourceFiles;
    }

    private void writeGeneratedSources(List<GenSrcFile> sources, Path srcPath) throws IOException {
        List<File> listFiles = new ArrayList<>();
        if (Files.exists(srcPath)) {
            File[] files = new File(String.valueOf(srcPath)).listFiles();
            if (files != null) {
                listFiles.addAll(Arrays.asList(files));
            }
        }

        for (File file : listFiles) {
            for (GenSrcFile gFile : sources) {
                if (file.getName().equals(gFile.getFileName())) {
                    int duplicateCount = 0;
                    setGeneratedFileName(listFiles, gFile, duplicateCount);
                }
            }
        }

        for (GenSrcFile file : sources) {
            Path filePath;
            if (file.getType().isOverwritable()) {
                filePath = Paths.get(srcPath.resolve(file.getFileName()).toFile().getCanonicalPath());
                writeFile(filePath, file.getContent());
            } else {
                filePath = srcPath.resolve(file.getFileName());
                if (Files.notExists(filePath)) {
                    String fileContent = file.getContent();
                    writeFile(filePath, fileContent);
                }
            }
        }
    }

    public static void setGeneratedFileName(List<File> listFiles, GenSrcFile gFile, int duplicateCount) {
        for (File listFile : listFiles) {
            String listFileName = listFile.getName();
            if (listFileName.contains(".") && ((listFileName.split("\\.")).length >= 2) &&
                    (listFileName.split("\\.")[0].equals(gFile.getFileName().split("\\.")[0]))) {
                duplicateCount = 1 + duplicateCount;
            }
        }
        gFile.setFileName(gFile.getFileName().split("\\.")[0] + "." + (duplicateCount) + "." +
                gFile.getFileName().split("\\.")[1]);
    }

    private String genServiceDeclaration(GeneratedFiles generatedFileDetails,
                                         Path serviceImplPath) throws IOException,
            WorkspaceDocumentException, EventSyncException, BallerinaOpenApiException {
        Path serviceObjPath = projectPath.resolve(SERVICE_OBJ_FILE);
        Project project = this.workspaceManager.loadProject(serviceObjPath);
        Package currentPackage = project.currentPackage();
        Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
        ModuleId moduleId = module.moduleId();
        DocumentId serviceObjDocId = DocumentId.create(serviceObjPath.toString(), moduleId);
        DocumentConfig documentConfig = DocumentConfig.from(
                serviceObjDocId, generatedFileDetails.serviceObjContent(), generatedFileDetails.serviceObjContent());
        module = module.modify().addDocument(documentConfig).apply();

        DocumentId typesDocId = DocumentId.create(generatedFileDetails.typesFile(), moduleId);
        DocumentConfig typeDocConfig = DocumentConfig.from(typesDocId, generatedFileDetails.typesContent(),
                generatedFileDetails.typesFile());
        module.modify().addDocument(typeDocConfig).apply();

        SemanticModel semanticModel = project.currentPackage().getCompilation().getSemanticModel(moduleId);
        TypeDefinitionSymbol symbol = getServiceTypeSymbol(semanticModel.moduleSymbols(), "OASServiceType");
        if (symbol == null) {
            throw new BallerinaOpenApiException("Cannot find service type definition");
        }

        TypeSymbol typeSymbol = symbol.typeDescriptor();
        if (typeSymbol.typeKind() != TypeDescKind.OBJECT) {
            throw new BallerinaOpenApiException("Cannot find service object type definition");
        }

        Map<String, MethodSymbol> methodSymbolMap = ((ObjectTypeSymbol) typeSymbol).methods();
        StringBuilder serviceImpl = new StringBuilder(IMPORT);
        serviceImpl.append(LS);
        serviceImpl.append(String.format(SERVICE_DECLARATION, port));
        serviceImpl.append(LS);
        for (Map.Entry<String, MethodSymbol> entry : methodSymbolMap.entrySet()) {
            MethodSymbol methodSymbol = entry.getValue();
            if (methodSymbol instanceof ResourceMethodSymbol resourceMethodSymbol) {
                serviceImpl.append(getMethodSignature(resourceMethodSymbol, getParentModuleName(symbol)));
            }
        }
        serviceImpl.append(CLOSE_BRACE).append(LS);
        String serviceImplContent = serviceImpl.toString();
        writeFile(serviceImplPath, serviceImplContent);
        return serviceImplContent;
    }

    private TypeDefinitionSymbol getServiceTypeSymbol(List<Symbol> symbols, String name) {
        for (Symbol symbol : symbols) {
            if (symbol.kind() == SymbolKind.TYPE_DEFINITION) {
                Optional<String> typeName = symbol.getName();
                if (typeName.isPresent() && typeName.get().equals(name)) {
                    return (TypeDefinitionSymbol) symbol;
                }
            }
        }
        return null;
    }

    private String getParentModuleName(Symbol symbol) {
        Optional<ModuleSymbol> module = symbol.getModule();
        return module.map(moduleSymbol -> moduleSymbol.id().toString()).orElse(null);
    }

    private String getMethodSignature(ResourceMethodSymbol resourceMethodSymbol, String parentModuleName) {
        String resourceSignature = resourceMethodSymbol.signature();
        if (Objects.nonNull(parentModuleName)) {
            resourceSignature = resourceSignature.replace(parentModuleName + ":", "");
        }
        return LS + "\t" + sanitizePackageNames(resourceSignature) + " {" + LS + LS + "\t}" + LS;
    }

    private String sanitizePackageNames(String input) {
        Pattern pattern = Pattern.compile("(\\w+)/(\\w+:)(\\d+\\.\\d+\\.\\d+):");
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("$2");
    }

    private static void writeFile(Path filePath, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filePath.toString(), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    private void checkOpenAPIVersion(OpenAPI openAPIDef) throws BallerinaOpenApiException {
        if (!SUPPORTED_OPENAPI_VERSIONS.contains(openAPIDef.getOpenapi())) {
            String sb = String.format("WARNING: The tool has not been tested with OpenAPI version %s. The generated " +
                    "code may potentially contain errors.", openAPIDef.getOpenapi()) + System.lineSeparator();
            throw new BallerinaOpenApiException(sb);
        }
    }

    private GeneratedFiles getFileDetails(List<GenSrcFile> files) throws BallerinaOpenApiException {
        String serviceObjContent = "";
        String serviceObjFile = "";
        String typesContent = "";
        String typesFile = "";

        boolean foundServiceObjFile = false;
        boolean foundTypesFile = false;

        for (GenSrcFile file : files) {
            if (file.getFileName().equals(SERVICE_OBJ_FILE)) {
                serviceObjFile = file.getFileName();
                serviceObjContent = file.getContent();
                foundServiceObjFile = true;
            } else if (file.getFileName().contains("types")) {
                typesFile = file.getFileName();
                typesContent = file.getContent();
                foundTypesFile = true;
            }
            if (foundServiceObjFile && foundTypesFile) {
                return new GeneratedFiles(serviceObjContent, serviceObjFile, typesContent, typesFile);
            }
        }

        throw new BallerinaOpenApiException("Necessary files are not generated.");
    }

    private record GeneratedFiles(String serviceObjContent, String serviceObjFile, String typesContent,
                                  String typesFile) {
    }
}
