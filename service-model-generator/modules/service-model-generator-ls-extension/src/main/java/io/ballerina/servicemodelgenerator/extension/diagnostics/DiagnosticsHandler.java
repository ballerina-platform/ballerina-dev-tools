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

package io.ballerina.servicemodelgenerator.extension.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.Types;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Diagnostics;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.FunctionSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceDesignerDiagnosticRequest;
import io.ballerina.servicemodelgenerator.extension.util.Utils;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Diagnostics handler for the service model generator.
 *
 * @since 2.3.0
 */
public class DiagnosticsHandler {

    final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private WorkspaceManager workspaceManager;
    private SemanticModel semanticModel;
    private Document document;

    public DiagnosticsHandler(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public JsonElement getDiagnostics(ServiceDesignerDiagnosticRequest request) throws WorkspaceDocumentException,
            EventSyncException {
        switch (request.operation()) {
            case "addResource" -> {
                FunctionSourceRequest function = gson.fromJson(request.request(), FunctionSourceRequest.class);
                handleAddResource(function);
                return gson.toJsonTree(function);
            }
        }
        return new JsonObject();
    }

    private void handleAddResource(FunctionSourceRequest funcRequest) throws WorkspaceDocumentException,
            EventSyncException {
        // Validations required
        Function function = funcRequest.function();

        String accessor = function.getAccessor().getValue().toLowerCase(Locale.ROOT);

        String resourceName = function.getName().getValue();
        List<Diagnostics.Info> diagnostics = new ArrayList<>();
        Set<String> paramNames = new HashSet<>();
        if (!validateResourcePath(resourceName, diagnostics, paramNames)) {
            function.getName().setDiagnostics(new Diagnostics(true, diagnostics));
            return;
        }

        Path filePath = Path.of(funcRequest.filePath());
        workspaceManager.loadProject(filePath);
        Optional<Document> doc = workspaceManager.document(filePath);
        Optional<SemanticModel> optionalSemanticModel = workspaceManager.semanticModel(filePath);
        if (doc.isEmpty() || optionalSemanticModel.isEmpty()) {
            return;
        }

        document = doc.get();
        semanticModel = optionalSemanticModel.get();

        // validate the uniqueness of the resource path
        NonTerminalNode node = findNonTerminalNode(funcRequest.codedata(), document);
        if (node.kind() != SyntaxKind.SERVICE_DECLARATION) {
            return;
        }
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;
        for (Node member: serviceDeclarationNode.members()) {
            if (!(member instanceof FunctionDefinitionNode functionDefinitionNode)) {
                continue;
            }
            if (functionDefinitionNode.qualifierList().stream().toList().contains(Qualifier.RESOURCE)) {
                continue;
            }
            // check if another function exist with the same accessor and function name
            if (!(Utils.getPath(functionDefinitionNode.relativeResourcePath()).equals(resourceName) &&
                    functionDefinitionNode.functionName().text().trim().equals(accessor))) {
                continue;
            }
            diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                    "Resource with the same name already exists"));
            return;
        }

        // load the document and see
        function.getName().setDiagnostics(null);

        Types types = semanticModel.types();
        TypeSymbol basicType =  types.builder().UNION_TYPE.withMemberTypes(types.BOOLEAN, types.INT,
                types.FLOAT, types.DECIMAL, types.STRING).build();

        TypeSymbol queryBasicType = types.builder().UNION_TYPE.withMemberTypes(basicType,
                types.builder().MAP_TYPE.withTypeParam(types.ANYDATA).build()).build();
        TypeSymbol queryTypeConstrain = types.builder().UNION_TYPE.withMemberTypes(types.NIL, queryBasicType,
                types.builder().ARRAY_TYPE.withType(queryBasicType).build()).build();

        TypeSymbol headerBasicType = types.builder().UNION_TYPE.withMemberTypes(types.NIL, basicType,
                types.builder().ARRAY_TYPE.withType(basicType).build(),
                types.builder().RECORD_TYPE.withRestField(basicType).build()).build();


        List<Parameter> parameters = function.getParameters();
        boolean hasHttpCaller = false;
        for (Parameter param: parameters) {
            if (param.isEnabled()) {
                String paramDef;
                Value paramType = param.getType();
                Value paramName = param.getName();
                if (paramType.getValue().equals("http:Caller")) {
                    hasHttpCaller = true;
                }

                if (!paramNames.add(paramName.getValue())) {
                    paramName.setDiagnostics(new Diagnostics(true, List.of(
                            new Diagnostics.Info(DiagnosticSeverity.ERROR, "Duplicate parameter name: "
                                    + paramName.getValue())
                    )));
                    return;
                }

                String httpParamType = param.getHttpParamType();
                if ("Query".equals(httpParamType)) {
                    types.getType(document, paramType.getValue()).ifPresent(typeSymbol -> {
                        if (!typeSymbol.subtypeOf(queryTypeConstrain)) {
                            // TODO: validate the enums
                            paramType.setDiagnostics(new Diagnostics(true, List.of(
                                    new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                            "Invalid type for query parameter: " + paramType.getValue())
                            )));
                        }
                    });
                    return;
                } else if ("Header".equals(httpParamType)) {
                    types.getType(document, paramType.getValue()).ifPresent(typeSymbol -> {
                        if (!typeSymbol.subtypeOf(headerBasicType)) {
                            paramType.setDiagnostics(new Diagnostics(true, List.of(
                                    new Diagnostics.Info(DiagnosticSeverity.ERROR,
                                            "Invalid type for query parameter: " + paramType.getValue())
                            )));
                        }
                    });
                    return;
                }
                paramType.setDiagnostics(null);
                paramName.setDiagnostics(null);
            }
        }

        FunctionReturnType returnType = function.getReturnType();
        // need to validate
    }

    public static boolean validateResourcePath(String path, List<Diagnostics.Info> diagnostics,
                                               Set<String> paramNames) {
        ResourcePathParser.ParseResult parseResult = ResourcePathParser.parseResourcePath(path);
        if (!parseResult.isValid()) {
            for (ResourcePathParser.ParseError error : parseResult.getErrors()) {
                diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR, error.getMessage()));
            }
            return false;
        }

        for (ResourcePathParser.Segment segment: parseResult.getSegments()) {
            if (segment instanceof ResourcePathParser.ParamSegment paramSegment) {
                String paramName = paramSegment.getParamName();
                if (!paramNames.add(paramName)) {
                    diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                            "Duplicate parameter name: " + paramName));
                    return false;
                }
            } else if (segment instanceof ResourcePathParser.ValueSegment valueSegment) {
                String value = valueSegment.getValue();
                if (value.isEmpty()) {
                    diagnostics.add(new Diagnostics.Info(DiagnosticSeverity.ERROR,
                            "Resource path contains invalid characters"));
                    return false;
                }
            }
        }

        return true;
    }

    private static NonTerminalNode findNonTerminalNode(Codedata codedata, Document document) {
        SyntaxTree syntaxTree = document.syntaxTree();
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        TextDocument textDocument = syntaxTree.textDocument();
        LineRange lineRange = codedata.getLineRange();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return modulePartNode.findNode(TextRange.from(start, end - start), true);
    }
}
