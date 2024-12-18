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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.SingleFileGenerator;
import io.ballerina.openapi.core.generators.common.TypeHandler;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.openapi.core.generators.common.model.Filter;
import io.ballerina.openapi.core.generators.common.model.GenSrcFile;
import io.ballerina.openapi.core.generators.service.ServiceGenerationHandler;
import io.ballerina.openapi.core.generators.service.model.OASServiceMetadata;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.ModuleName;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.swagger.v3.oas.models.OpenAPI;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;
import org.ballerinalang.langserver.common.utils.DefaultValueGenerationUtil;
import org.ballerinalang.langserver.common.utils.RecordUtil;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
 * @since 2.0.0
 */
public class OpenApiServiceGenerator {

    public static final String MAIN_BAL = "main.bal";
    private final WorkspaceManager workspaceManager;
    private final Path oAContractPath;
    private final Path projectPath;
    private final Gson gson;
    public static final List<String> SUPPORTED_OPENAPI_VERSIONS = List.of("2.0", "3.0.0", "3.0.1", "3.0.2", "3.0.3",
            "3.1.0");
    public static final String LS = System.lineSeparator();
    public static final String OPEN_BRACE = "{";
    public static final String CLOSE_BRACE = "}";
    public static final String SPACE = " ";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String BALLERINA_HTTP = "ballerina/http";
    public static final String BALLERINA_LANG = "ballerina/lang";
    public static final String DEFAULT_HTTP_RESPONSE = "DefaultStatusCodeResponse";
    public static final String DEFAULT_HTTP_RESPONSE_VALUE = "status: new (0)";
    public static final String SERVICE_DECLARATION = "service %s on %s {";

    public OpenApiServiceGenerator(Path oAContractPath, Path projectPath, WorkspaceManager workspaceManager) {
        this.oAContractPath = oAContractPath;
        this.projectPath = projectPath;
        this.workspaceManager = workspaceManager;
        this.gson = new Gson();
    }

    public JsonElement generateService(String typeName, List<String> listeners) throws IOException,
            BallerinaOpenApiException, FormatterException,
            WorkspaceDocumentException, EventSyncException {
        Filter filter = new Filter(new ArrayList<>(), new ArrayList<>());

        List<Diagnostic> diagnostics = new ArrayList<>();
        GenSrcFile serviceTypeFile = generateServiceType(oAContractPath, typeName, filter, diagnostics);
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

        Path mainFile = projectPath.resolve(MAIN_BAL);
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        Project project = this.workspaceManager.loadProject(mainFile);
        Optional<Document> document = this.workspaceManager.document(mainFile);
        if (document.isPresent()) {
            String serviceImplContent = genServiceImplementation(serviceTypeFile, typeName, listeners, project,
                    mainFile);
            ModulePartNode modulePartNode = document.get().syntaxTree().rootNode();
            LinePosition startPos = LinePosition.from(modulePartNode.lineRange().endLine().line() + 1, 0);
            List<TextEdit> textEdits = new ArrayList<>();
            textEdits.add(new TextEdit(CommonUtils.toRange(startPos), serviceImplContent));
            textEditsMap.put(mainFile, textEdits);
        }
        List<TextEdit> textEdits = new ArrayList<>();
        textEdits.add(new TextEdit(CommonUtils.toRange(LinePosition.from(0, 0)), serviceTypeFile.getContent()));
        textEditsMap.put(projectPath.resolve(serviceTypeFile.getFileName()), textEdits);
        return gson.toJsonTree(textEditsMap);
    }

    public GenSrcFile generateServiceType(Path openAPI, String typeName, Filter filter, List<Diagnostic> diagnostics)
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
                .withServiceObjectTypeName(typeName)
                .withSrcFile("service_contract_" + typeName + ".bal")
                .build();
        TypeHandler.createInstance(openAPIDef, true);
        ServiceGenerationHandler serviceGenerationHandler = new ServiceGenerationHandler();
        SyntaxTree syntaxTree = serviceGenerationHandler.generateSingleSyntaxTree(oasServiceMetadata);
        if (!oasServiceMetadata.generateWithoutDataBinding()) {
            syntaxTree = SingleFileGenerator.combineSyntaxTrees(syntaxTree,
                    TypeHandler.getInstance().generateTypeSyntaxTree());
        }
        GenSrcFile genSrcFile = new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, oasServiceMetadata.getSrcPackage(),
                oasServiceMetadata.getSrcFile(),
                (oasServiceMetadata.getLicenseHeader().isBlank() ? DEFAULT_FILE_HEADER :
                        oasServiceMetadata.getLicenseHeader()) + Formatter.format(syntaxTree).toSourceCode());

        diagnostics.addAll(serviceGenerationHandler.getDiagnostics());
        diagnostics.addAll(TypeHandler.getInstance().getDiagnostics());
        return genSrcFile;
    }

    private String genServiceImplementation(GenSrcFile serviceType, String typeName, List<String> listeners,
                                            Project project, Path mainFile) throws BallerinaOpenApiException {
        Package currentPackage = project.currentPackage();
        Module module = currentPackage.module(ModuleName.from(currentPackage.packageName()));
        ModuleId moduleId = module.moduleId();
        DocumentId serviceObjDocId = DocumentId.create(mainFile.toString(), moduleId);
        DocumentConfig documentConfig = DocumentConfig.from(
                serviceObjDocId, serviceType.getContent(), serviceType.getFileName());
        module.modify().addDocument(documentConfig).apply();

        SemanticModel semanticModel = project.currentPackage().getCompilation().getSemanticModel(moduleId);
        TypeDefinitionSymbol symbol = getServiceTypeSymbol(semanticModel.moduleSymbols(), typeName);
        if (symbol == null) {
            throw new BallerinaOpenApiException("Cannot find service type definition");
        }

        TypeSymbol typeSymbol = symbol.typeDescriptor();
        if (typeSymbol.typeKind() != TypeDescKind.OBJECT) {
            throw new BallerinaOpenApiException("Cannot find service object type definition");
        }

        Map<String, MethodSymbol> methodSymbolMap = ((ObjectTypeSymbol) typeSymbol).methods();
        StringBuilder serviceImpl = new StringBuilder();
        serviceImpl.append(String.format(SERVICE_DECLARATION, typeName, String.join(", ", listeners)));
        serviceImpl.append(LS);
        for (Map.Entry<String, MethodSymbol> entry : methodSymbolMap.entrySet()) {
            MethodSymbol methodSymbol = entry.getValue();
            if (methodSymbol instanceof ResourceMethodSymbol resourceMethodSymbol) {
                serviceImpl.append(getResourceFunction(resourceMethodSymbol, getParentModuleName(symbol)));
            }
        }
        serviceImpl.append(CLOSE_BRACE).append(LS);
        return serviceImpl.toString();
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

    private String getResourceFunction(ResourceMethodSymbol resourceMethodSymbol, String parentModuleName)
            throws BallerinaOpenApiException {
        String resourceSignature = resourceMethodSymbol.signature();
        if (Objects.nonNull(parentModuleName)) {
            resourceSignature = resourceSignature.replace(parentModuleName + COLON, "");
        }
        if (resourceSignature.contains(BALLERINA_LANG)) {
            resourceSignature = resourceSignature.replace(BALLERINA_LANG + ".", "");
            resourceSignature = resourceSignature.replaceAll("\\d+\\.\\d+\\.\\d+:", "");
        }
        return genResourceFunctionBody(resourceMethodSymbol, resourceSignature);
    }

    private String genResourceFunctionBody(ResourceMethodSymbol resourceMethodSymbol, String resourceSignature)
            throws BallerinaOpenApiException {
        FunctionTypeSymbol functionTypeSymbol = resourceMethodSymbol.typeDescriptor();
        Optional<TypeSymbol> optReturnTypeSymbol = functionTypeSymbol.returnTypeDescriptor();
        String possibleErrorReturningType = "()";
        if (optReturnTypeSymbol.isPresent()) {
            TypeSymbol typeSymbol = optReturnTypeSymbol.get();
            possibleErrorReturningType = getPossibleErrorHttpResponse(typeSymbol);
            if (possibleErrorReturningType.isEmpty()) {
                possibleErrorReturningType = getPossibleErrorReturningValue(typeSymbol);
            }
            if (possibleErrorReturningType.isEmpty()) {
                possibleErrorReturningType = getDefaultValue(typeSymbol);
            }
            if (possibleErrorReturningType.isEmpty()) {
                throw new BallerinaOpenApiException("Cannot find default return value for: "
                        + resourceMethodSymbol.signature() + "and " + typeSymbol.signature());
            }
        }
        return LS + "\t" + sanitizePackageNames(resourceSignature) + " {" + LS + "\t\tdo {" + LS + "\t\t} on fail " +
                "error e {" + LS + "\t\t\t" + "return " + possibleErrorReturningType + ";" + LS + "\t\t}" +
                LS + "\t}" + LS;
    }

    private String getPossibleErrorHttpResponse(TypeSymbol typeSymbol) {
        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.UNION) {
            List<TypeSymbol> typeSymbols = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors();
            for (TypeSymbol symbol : typeSymbols) {
                String possibleErrorReturningType = getPossibleErrorHttpResponse(symbol);
                if (!possibleErrorReturningType.isEmpty()) {
                    return possibleErrorReturningType;
                }
            }
        } else if (kind == TypeDescKind.TYPE_REFERENCE) {
            return getPossibleErrorHttpResponse(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        } else if (kind == TypeDescKind.RECORD) {
            String typeStr = typeSymbol.signature();
            if (!typeStr.contains(BALLERINA_HTTP)) {
                return "";
            }
            if (typeStr.contains("InternalServerError")) {
                return "http:INTERNAL_SERVER_ERROR";
            }
            if (typeStr.contains("NotFound")) {
                return "http:NOT_FOUND";
            }
            if (typeStr.contains("MethodNotAllowed")) {
                return "http:METHOD_NOT_ALLOWED";
            }
            if (typeStr.contains("BadRequest")) {
                return "http:BAD_REQUEST";
            }
            // TODO: Add more possible status codes
        }
        return "";
    }

    private String getPossibleErrorReturningValue(TypeSymbol typeSymbol) {
        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.UNION) {
            List<TypeSymbol> typeSymbols = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors();
            for (TypeSymbol symbol : typeSymbols) {
                String possibleErrorReturningType = getPossibleErrorReturningValue(symbol);
                if (!possibleErrorReturningType.isEmpty()) {
                    return possibleErrorReturningType;
                }
            }
            return "";
        } else if (kind == TypeDescKind.TYPE_REFERENCE) {
            return getPossibleErrorReturningValue(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        } else if (kind == TypeDescKind.RECORD) {
            RecordTypeSymbol recordTypeSymbol = (RecordTypeSymbol) typeSymbol;
            List<TypeSymbol> includedTypeSymbols = (recordTypeSymbol).typeInclusions();
            for (TypeSymbol includedTypeSymbol : includedTypeSymbols) {
                String signature = includedTypeSymbol.signature();
                if (signature.contains(BALLERINA_HTTP) && signature.contains(DEFAULT_HTTP_RESPONSE)) {
                    return getDefaultRecordValue(recordTypeSymbol);
                }
            }
        }
        return "";
    }

    private String getDefaultRecordValue(RecordTypeSymbol recordTypeSymbol) {
        StringBuilder sb = new StringBuilder();
        sb.append(OPEN_BRACE);
        Map<String, RecordFieldSymbol> fieldDescriptors = recordTypeSymbol.fieldDescriptors();
        for (Map.Entry<String, RecordFieldSymbol> fieldDescriptor : fieldDescriptors.entrySet()) {
            String key = fieldDescriptor.getKey();
            if (key.contains("status") || key.contains("mediaType") || key.contains("headers")) {
                continue;
            }
            sb.append(key).append(COLON).append(SPACE)
                    .append(getDefaultValue(fieldDescriptor.getValue().typeDescriptor())).append(COMMA)
                    .append(SPACE);
        }
        sb.append(DEFAULT_HTTP_RESPONSE_VALUE).append(CLOSE_BRACE);
        return sb.toString();
    }

    private String getDefaultValue(TypeSymbol typeSymbol) {
        TypeDescKind kind = typeSymbol.typeKind();
        if (kind == TypeDescKind.UNION) {
            List<TypeSymbol> typeSymbols = ((UnionTypeSymbol) typeSymbol).memberTypeDescriptors();
            for (TypeSymbol symbol : typeSymbols) {
                String possibleErrorReturningType = getDefaultValue(symbol);
                if (!possibleErrorReturningType.isEmpty()) {
                    return possibleErrorReturningType;
                }
            }
            return "";
        } else if (kind == TypeDescKind.TYPE_REFERENCE) {
            return getDefaultValue(((TypeReferenceTypeSymbol) typeSymbol).typeDescriptor());
        } else if (kind == TypeDescKind.RECORD) {
            StringBuilder sb = new StringBuilder();
            sb.append(OPEN_BRACE);
            Map<String, RecordFieldSymbol> fieldDescriptors = ((RecordTypeSymbol) typeSymbol).fieldDescriptors();
            sb.append(RecordUtil.getFillAllRecordFieldInsertText(fieldDescriptors));
            sb.append(CLOSE_BRACE);
            return sb.toString();
        } else if (kind == TypeDescKind.ANYDATA) {
            return "\"\"";
        }
        return DefaultValueGenerationUtil.getDefaultValueForType(typeSymbol).orElse("");
    }

    private String sanitizePackageNames(String input) {
        Pattern pattern = Pattern.compile("(\\w+)/(\\w+:)(\\d+\\.\\d+\\.\\d+):");
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("$2");
    }

    private void checkOpenAPIVersion(OpenAPI openAPIDef) throws BallerinaOpenApiException {
        if (!SUPPORTED_OPENAPI_VERSIONS.contains(openAPIDef.getOpenapi())) {
            String sb = String.format("WARNING: The tool has not been tested with OpenAPI version %s. The generated " +
                    "code may potentially contain errors.", openAPIDef.getOpenapi()) + System.lineSeparator();
            throw new BallerinaOpenApiException(sb);
        }
    }
}
