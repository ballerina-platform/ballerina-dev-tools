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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.openapi.core.generators.client.BallerinaClientGenerator;
import io.ballerina.openapi.core.generators.client.diagnostic.ClientDiagnostic;
import io.ballerina.openapi.core.generators.client.exception.ClientException;
import io.ballerina.openapi.core.generators.client.model.OASClientConfig;
import io.ballerina.openapi.core.generators.common.GeneratorUtils;
import io.ballerina.openapi.core.generators.common.TypeHandler;
import io.ballerina.openapi.core.generators.common.exception.BallerinaOpenApiException;
import io.ballerina.openapi.core.generators.common.model.Filter;
import io.ballerina.openapi.core.generators.common.model.GenSrcFile;
import io.ballerina.toml.syntax.tree.DocumentMemberDeclarationNode;
import io.ballerina.toml.syntax.tree.DocumentNode;
import io.ballerina.toml.syntax.tree.KeyValueNode;
import io.ballerina.toml.syntax.tree.SyntaxKind;
import io.ballerina.toml.syntax.tree.SyntaxTree;
import io.ballerina.toml.syntax.tree.TableArrayNode;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;
import io.swagger.v3.oas.models.OpenAPI;
import org.ballerinalang.formatter.core.Formatter;
import org.ballerinalang.formatter.core.FormatterException;
import org.eclipse.lsp4j.TextEdit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.openapi.core.generators.common.GeneratorConstants.CLIENT_FILE_NAME;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.TYPE_FILE_NAME;
import static io.ballerina.openapi.core.generators.common.GeneratorConstants.UTIL_FILE_NAME;

/**
 * Generates client from the OpenAPI contract.
 *
 * @since 2.0.0
 */
public class OpenAPIClientGenerator {
    public static final String BALLERINA_TOML = "Ballerina.toml";
    private final Gson gson;
    private final Path oAContractPath;
    private final Path projectPath;
    private static final String LS = System.lineSeparator();

    public OpenAPIClientGenerator(Path oAContractPath, Path projectPath) {
        this.gson = new Gson();
        this.oAContractPath = oAContractPath;
        this.projectPath = projectPath;
    }

    public JsonElement genClient(String module) throws IOException, BallerinaOpenApiException, ClientException,
            FormatterException {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        boolean isModuleExists = genBalTomlTableEntry(module, textEditsMap);
        genClientSource(module, textEditsMap);

        ClientSource clientSource = new ClientSource(isModuleExists, textEditsMap);
        return gson.toJsonTree(clientSource);
    }

    public JsonArray getModules() throws IOException {
        Path tomlPath = this.projectPath.resolve(BALLERINA_TOML);
        TextDocument configDocument = TextDocuments.from(Files.readString(tomlPath));
        SyntaxTree syntaxTree = SyntaxTree.from(configDocument);
        DocumentNode rootNode = syntaxTree.rootNode();

        List<String> modules = new ArrayList<>();
        for (DocumentMemberDeclarationNode node : rootNode.members()) {
            if (node.kind() != SyntaxKind.TABLE_ARRAY) {
                continue;
            }
            TableArrayNode tableArrayNode = (TableArrayNode) node;
            if (!tableArrayNode.identifier().toSourceCode().equals("tool.openapi")) {
                continue;
            }

            for (KeyValueNode field : tableArrayNode.fields()) {
                String identifier = field.identifier().toSourceCode();
                if (identifier.trim().equals("targetModule")) {
                    String fieldValue = field.value().toSourceCode().trim();
                    int endCharIndex = fieldValue.length() - 1;
                    if (fieldValue.endsWith(System.lineSeparator())) {
                        endCharIndex = endCharIndex - 1;
                    }
                    modules.add(fieldValue.substring(1, endCharIndex));
                }
            }
        }
        return gson.toJsonTree(modules).getAsJsonArray();
    }

    private boolean genBalTomlTableEntry(String module, Map<Path, List<TextEdit>> textEditsMap) throws IOException {
        Path tomlPath = this.projectPath.resolve(BALLERINA_TOML);
        TextDocument configDocument = TextDocuments.from(Files.readString(tomlPath));
        SyntaxTree syntaxTree = SyntaxTree.from(configDocument);
        DocumentNode rootNode = syntaxTree.rootNode();

        LineRange lineRange = null;
        for (DocumentMemberDeclarationNode node : rootNode.members()) {
            if (node.kind() != SyntaxKind.TABLE_ARRAY) {
                continue;
            }
            TableArrayNode tableArrayNode = (TableArrayNode) node;
            if (!tableArrayNode.identifier().toSourceCode().equals("tool.openapi")) {
                continue;
            }

            for (KeyValueNode field : tableArrayNode.fields()) {
                String identifier = field.identifier().toSourceCode();
                if (identifier.trim().equals("targetModule")) {
                    if (field.value().toSourceCode().contains("\"" + module + "\"")) {
                        lineRange = tableArrayNode.lineRange();
                        break;
                    }
                }
            }
        }

        String tomlEntry = getTomlEntry(module);
        List<TextEdit> textEdits = new ArrayList<>();
        textEditsMap.put(tomlPath, textEdits);
        if (lineRange != null) {
            textEdits.add(new TextEdit(CommonUtils.toRange(lineRange), tomlEntry));
        } else {
            LinePosition startPos = LinePosition.from(rootNode.lineRange().endLine().line() + 1, 0);
            textEdits.add(new TextEdit(CommonUtils.toRange(startPos), tomlEntry));
        }
        return lineRange != null;
    }

    private void genClientSource(String module, Map<Path, List<TextEdit>> textEditsMap) throws IOException,
            BallerinaOpenApiException, ClientException, FormatterException {
        OpenAPI openAPI = GeneratorUtils.normalizeOpenAPI(oAContractPath, false, false);
        if (openAPI.getInfo() == null) {
            throw new BallerinaOpenApiException("Info section of the definition file cannot be empty/null: " +
                    openAPI);
        }

        Filter filter = new Filter();
        OASClientConfig clientConfig = new OASClientConfig.Builder().withFilters(filter).withOpenAPI(openAPI).build();
        TypeHandler.createInstance(clientConfig.getOpenAPI(), clientConfig.isNullable());
        BallerinaClientGenerator balClientGenerator = new BallerinaClientGenerator(clientConfig);
        io.ballerina.compiler.syntax.tree.SyntaxTree syntaxTree = balClientGenerator.generateSyntaxTree();
        List<ClientDiagnostic> clientDiagnostic = balClientGenerator.getDiagnostics();

        if (clientDiagnostic.stream().anyMatch(
                diagnostic -> diagnostic.getDiagnosticSeverity() == DiagnosticSeverity.ERROR)) {
            throw new ClientException("Error occurred while generating client");
        }

        List<TypeDefinitionNode> authNodes = balClientGenerator.getBallerinaAuthConfigGenerator()
                .getAuthRelatedTypeDefinitionNodes();
        for (TypeDefinitionNode typeDef : authNodes) {
            TypeHandler.getInstance().addTypeDefinitionNode(typeDef.typeName().text(), typeDef);
        }

        String licenseContent = clientConfig.getLicense();
        String licenseHeader = licenseContent == null || licenseContent.isBlank() ? "" : licenseContent + LS;

        List<GenSrcFile> sourceFiles = genClientSourceFiles(syntaxTree, licenseHeader, balClientGenerator);
        Path outputPath = projectPath.resolve("generated").resolve(module);
        for (GenSrcFile sourceFile : sourceFiles) {
            List<TextEdit> textEdits = new ArrayList<>();
            textEdits.add(new TextEdit(CommonUtils.toRange(LinePosition.from(0, 0)), sourceFile.getContent()));
            textEditsMap.put(outputPath.resolve(sourceFile.getFileName()), textEdits);
        }
    }

    private List<GenSrcFile> genClientSourceFiles(io.ballerina.compiler.syntax.tree.SyntaxTree syntaxTree,
                                      String licenseHeader, BallerinaClientGenerator ballerinaClientGenerator) throws
            FormatterException, IOException {
        List<GenSrcFile> sourceFiles = new ArrayList<>();
        String mainContent = Formatter.format(syntaxTree).toSourceCode();
        sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.GEN_SRC, null, CLIENT_FILE_NAME,
                licenseHeader + mainContent));
        String utilContent = Formatter.format(
                ballerinaClientGenerator.getBallerinaUtilGenerator().generateUtilSyntaxTree()).toString();
        if (!utilContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.UTIL_SRC, null, UTIL_FILE_NAME,
                    licenseHeader + utilContent));
        }
        io.ballerina.compiler.syntax.tree.SyntaxTree schemaSyntaxTree = TypeHandler.getInstance()
                .generateTypeSyntaxTree();
        String schemaContent = Formatter.format(schemaSyntaxTree).toSourceCode();
        if (!schemaContent.isBlank()) {
            sourceFiles.add(new GenSrcFile(GenSrcFile.GenFileType.MODEL_SRC, null, TYPE_FILE_NAME,
                    licenseHeader + schemaContent));
        }
        return sourceFiles;
    }

    private String getTomlEntry(String module) {
        String moduleWithQuotes = "\"" + module + "\"";
        return LS + "[[tool.openapi]]" + LS +
                "id" + " = " + moduleWithQuotes + LS +
                "targetModule" + " = " + moduleWithQuotes + LS +
                "filePath" + " = " + "\"" +
                oAContractPath.toAbsolutePath().toString().replace("\\", "\\\\") + "\"" + LS;
    }

    private record ClientSource(boolean isModuleExists, Map<Path, List<TextEdit>> textEditsMap) {
    }
}
