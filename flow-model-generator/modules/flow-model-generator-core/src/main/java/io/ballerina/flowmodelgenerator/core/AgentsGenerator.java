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
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.Item;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.PropertyCodedata;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.FlowNodeUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This class is responsible for managing agents.
 *
 * @since 2.0.0
 */
public class AgentsGenerator {

    public static final String MODEL = "Model";
    public static final String TOOL_ANNOTATION = "Tool";
    public static final String TARGET_TYPE = "targetType";
    private final Gson gson;
    private final SemanticModel semanticModel;
    private static final String BALLERINAX = "ballerinax";
    private static final String AI_AGENT = "ai.agent";
    private static final String INIT = "init";
    private static final String AGENT_FILE = "agents.bal";
    public static final String AGENT = "Agent";
    private static final String BALLERINA_ORG = "ballerina";
    private static final String HTTP_MODULE = "http";
    private static final List<String> HTTP_REMOTE_METHOD_SKIP_LIST = List.of("get", "put", "post", "head",
            "delete", "patch", "options");

    public AgentsGenerator() {
        this.gson = new Gson();
        this.semanticModel = null;
    }

    public AgentsGenerator(SemanticModel semanticModel) {
        this.gson = new Gson();
        this.semanticModel = semanticModel;
    }

    public JsonArray getAllAgents(SemanticModel agentSymbol) {
        List<Codedata> agents = new ArrayList<>();
        for (Symbol symbol : agentSymbol.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.CLASS) {
                continue;
            }
            ClassSymbol classSymbol = (ClassSymbol) symbol;
            if (classSymbol.qualifiers().contains(Qualifier.CLIENT) && classSymbol.getName().orElse("").equals(AGENT)) {
                Optional<ModuleSymbol> optModule = classSymbol.getModule();
                if (optModule.isEmpty()) {
                    throw new IllegalStateException("Agent module id not found");
                }
                ModuleID id = optModule.get().id();

                agents.add(new Codedata.Builder<>(null).node(NodeKind.AGENT)
                        .org(id.orgName())
                        .module(id.packageName())
                        .version(id.version())
                        .object(classSymbol.getName().orElse(AGENT))
                        .symbol(INIT)
                        .build());
            }
        }
        return gson.toJsonTree(agents).getAsJsonArray();
    }

    public JsonArray getAllModels(SemanticModel agentSymbol) {
        List<ClassSymbol> modelSymbols = new ArrayList<>();
        for (Symbol symbol : agentSymbol.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.CLASS) {
                continue;
            }
            ClassSymbol classSymbol = (ClassSymbol) symbol;
            if (!classSymbol.qualifiers().contains(Qualifier.CLIENT)) {
                continue;
            }
            List<TypeSymbol> inclusionsTypes = classSymbol.typeInclusions();
            for (TypeSymbol typeSymbol : inclusionsTypes) {
                if (typeSymbol.getName().isPresent() && typeSymbol.getName().get().equals(MODEL)) {
                    modelSymbols.add(classSymbol);
                    break;
                }
            }
        }

        List<Codedata> models = new ArrayList<>();
        for (ClassSymbol model : modelSymbols) {
            Optional<ModuleSymbol> optModule = model.getModule();
            if (optModule.isEmpty()) {
                throw new IllegalStateException("Agent module id not found");
            }
            ModuleID id = optModule.get().id();
            models.add(new Codedata.Builder<>(null).node(NodeKind.CLASS_INIT)
                    .org(id.orgName())
                    .module(id.packageName())
                    .version(id.version())
                    .object(model.getName().orElse(MODEL))
                    .symbol(INIT)
                    .build());
        }
        return gson.toJsonTree(models).getAsJsonArray();
    }

    public JsonArray getModels() {
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        List<String> models = new ArrayList<>();
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() != SymbolKind.VARIABLE) {
                continue;
            }
            VariableSymbol variableSymbol = (VariableSymbol) moduleSymbol;
            TypeSymbol typeSymbol = CommonUtils.getRawType(variableSymbol.typeDescriptor());
            if (typeSymbol.kind() != SymbolKind.CLASS) {
                continue;
            }
            List<TypeSymbol> typeInclusions = ((ClassSymbol) typeSymbol).typeInclusions();
            for (TypeSymbol typeInclusion : typeInclusions) {
                Optional<String> optName = typeInclusion.getName();
                if (optName.isPresent() && optName.get().equals(MODEL)) {
                    models.add(variableSymbol.getName().orElse(""));
                }
            }
        }
        return gson.toJsonTree(models).getAsJsonArray();
    }

    public JsonArray getTools(SemanticModel semanticModel) {
        List<Symbol> moduleSymbols = semanticModel.moduleSymbols();
        List<String> functionNames = new ArrayList<>();
        TypeSymbol anydata = semanticModel.types().ANYDATA;
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() != SymbolKind.FUNCTION) {
                continue;
            }

            FunctionSymbol functionSymbol = (FunctionSymbol) moduleSymbol;
            if (!functionSymbol.qualifiers().contains(Qualifier.ISOLATED)) {
                continue;
            }

            FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
            Optional<List<ParameterSymbol>> optParams = functionTypeSymbol.params();
            if (optParams.isPresent()) {
                boolean isAnydataSubType = true;
                for (ParameterSymbol parameterSymbol : optParams.get()) {
                    if (!CommonUtils.subTypeOf(parameterSymbol.typeDescriptor(), anydata)) {
                        isAnydataSubType = false;
                        break;
                    }
                }
                if (!isAnydataSubType) {
                    continue;
                }
            }
            Optional<TypeSymbol> optReturnTypeSymbol = functionTypeSymbol.returnTypeDescriptor();
            if (optReturnTypeSymbol.isPresent()) {
                if (!CommonUtils.subTypeOf(optReturnTypeSymbol.get(), anydata)) {
                    continue;
                }
            }
            if (isToolAnnotated(functionSymbol)) {
                functionNames.add(moduleSymbol.getName().orElse(""));
            }
        }

        return gson.toJsonTree(functionNames).getAsJsonArray();
    }

    public JsonElement genTool(JsonElement node, String toolName, String connectionName, String description,
                               Path filePath, WorkspaceManager workspaceManager) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        NodeKind nodeKind = flowNode.codedata().node();
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, workspaceManager, filePath);
        List<String> args = new ArrayList<>();
        String path = flowNode.metadata().icon();
        if (nodeKind == NodeKind.FUNCTION_DEFINITION) {
            sourceBuilder.token()
                    .name("@agent:Tool").
                    name(System.lineSeparator());
            sourceBuilder.token()
                    .name("@display {")
                    .name("label: \"\",")
                    .name("iconPath: \"")
                    .name(path == null ? "" : path)
                    .name("\"}")
                    .name(System.lineSeparator());

            sourceBuilder.token().keyword(SyntaxKind.ISOLATED_KEYWORD).keyword(SyntaxKind.FUNCTION_KEYWORD);
            sourceBuilder.token().name(toolName).keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            Optional<Property> parameters = flowNode.getProperty(Property.PARAMETERS_KEY);
            if (parameters.isPresent() && parameters.get().value() instanceof Map<?, ?> paramMap) {
                List<String> paramList = new ArrayList<>();
                for (Object obj : paramMap.values()) {
                    Property paramProperty = gson.fromJson(gson.toJsonTree(obj), Property.class);
                    if (!(paramProperty.value() instanceof Map<?, ?> paramData)) {
                        continue;
                    }
                    Map<String, Property> paramProperties = gson.fromJson(gson.toJsonTree(paramData),
                            FormBuilder.NODE_PROPERTIES_TYPE);

                    String paramType = paramProperties.get(Property.TYPE_KEY).value().toString();
                    String paramName = paramProperties.get(Property.VARIABLE_KEY).value().toString();
                    args.add(paramName);
                    paramList.add(paramType + " " + paramName);
                }
                sourceBuilder.token().name(String.join(", ", paramList));
            }
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

            Optional<Property> returnType = flowNode.getProperty(Property.TYPE_KEY);
            boolean hasReturn = returnType.isPresent() && !returnType.get().value().toString().isEmpty();
            if (hasReturn) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(returnType.get().value().toString());
            }

            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);
            if (hasReturn) {
                sourceBuilder.token()
                        .name(returnType.get().value().toString())
                        .whiteSpace()
                        .name("result")
                        .whiteSpace()
                        .keyword(SyntaxKind.EQUAL_TOKEN);
            }
            Optional<Property> optFuncName = flowNode.getProperty(Property.FUNCTION_NAME_KEY);
            if (optFuncName.isEmpty()) {
                throw new IllegalStateException("Function name is not present");
            }
            sourceBuilder.token()
                    .name(optFuncName.get().value().toString())
                    .keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            sourceBuilder.token()
                    .name(String.join(", ", args))
                    .keyword(SyntaxKind.CLOSE_PAREN_TOKEN).endOfStatement();

            if (hasReturn) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURN_KEYWORD)
                        .name("result")
                        .endOfStatement();
            }

            sourceBuilder.token()
                    .keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
            sourceBuilder.textEdit(false, AGENT_FILE);
            Map<Path, List<TextEdit>> textEdits = sourceBuilder.build();
            List<TextEdit> te = new ArrayList<>();
            Path p = addIsolateKeyword(optFuncName.get().value().toString().trim(), filePath, te);
            if (p != null) {
                textEdits.put(p, te);
            }
            return gson.toJsonTree(textEdits);
        } else if (nodeKind == NodeKind.REMOTE_ACTION_CALL) {
            boolean hasDescription = genDescription(description, flowNode, sourceBuilder);
            Map<String, Property> properties = flowNode.properties();
            Set<String> keys = new LinkedHashSet<>(properties != null ? properties.keySet() : Set.of());
            keys.removeAll(Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY, TARGET_TYPE, Property.CONNECTION_KEY,
                    Property.CHECK_ERROR_KEY));
            List<String> paramList = new ArrayList<>();
            for (String key : keys) {
                Property property = properties.get(key);
                if (property == null) {
                    continue;
                }
                if (hasDescription) {
                    sourceBuilder.token().parameterDoc(key, property.metadata().description());
                }
                String paramType = property.valueTypeConstraint().toString();
                paramList.add(paramType + " " + key);
            }

            Optional<Property> optReturnType = flowNode.getProperty(Property.TYPE_KEY);
            String returnType = "";
            if (optReturnType.isPresent()) {
                Property returnProperty = optReturnType.get();
                if (flowNode.getProperty(TARGET_TYPE).isPresent()) {
                    returnType = "json";
                } else {
                    returnType = returnProperty.value().toString();
                }
                sourceBuilder.token().returnDoc(returnProperty.metadata().description());
            }

            sourceBuilder.token()
                    .name("@agent:Tool").
                    name(System.lineSeparator());
            sourceBuilder.token()
                    .name("@display {")
                    .name("label: \"\",")
                    .name("iconPath: \"")
                    .name(path == null ? "" : path)
                    .name("\"}")
                    .name(System.lineSeparator());

            sourceBuilder.token().keyword(SyntaxKind.ISOLATED_KEYWORD).keyword(SyntaxKind.FUNCTION_KEYWORD);
            sourceBuilder.token().name(toolName).keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            sourceBuilder.token().name(String.join(", ", paramList));
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);


            if (!returnType.isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(returnType);
                if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
                    sourceBuilder.token().keyword(SyntaxKind.PIPE_TOKEN).keyword(SyntaxKind.ERROR_KEYWORD);
                }
            }

            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);

            if (!returnType.isEmpty()) {
                sourceBuilder.token().expressionWithType(returnType,
                        flowNode.getProperty(Property.VARIABLE_KEY).orElseThrow()).keyword(SyntaxKind.EQUAL_TOKEN);
            }
            if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
                sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
            }
            sourceBuilder.token()
                    .name(connectionName)
                    .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                    .name(flowNode.metadata().label())
                    .stepOut()
                    .functionParameters(flowNode, Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY,
                            Property.CONNECTION_KEY, Property.CHECK_ERROR_KEY));

            if (!returnType.isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURN_KEYWORD)
                        .name(flowNode.getProperty(Property.VARIABLE_KEY).get().value().toString())
                        .endOfStatement();
            }
            sourceBuilder.token()
                    .keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
            sourceBuilder.textEdit(false, AGENT_FILE);
            return gson.toJsonTree(sourceBuilder.build());
        } else if (nodeKind == NodeKind.RESOURCE_ACTION_CALL) {
            boolean hasDescription = genDescription(description, flowNode, sourceBuilder);
            Map<String, Property> properties = flowNode.properties();
            Set<String> keys = new LinkedHashSet<>(properties != null ? properties.keySet() : Set.of());
            Set<String> ignoredKeys = new HashSet<>(List.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY,
                    Property.TYPE_KEY, TARGET_TYPE, Property.RESOURCE_PATH_KEY, Property.CHECK_ERROR_KEY));
            keys.removeAll(ignoredKeys);
            List<String> paramList = new ArrayList<>();
            for (String key : keys) {
                Property property = properties.get(key);
                if (property == null) {
                    continue;
                }
                if (hasDescription) {
                    sourceBuilder.token().parameterDoc(key, property.metadata().description());
                }
                String paramType = property.valueTypeConstraint().toString();
                paramList.add(paramType + " " + key);
            }

            sourceBuilder.token()
                    .name("@agent:Tool")
                    .name(System.lineSeparator());
            sourceBuilder.token()
                    .name("@display {")
                    .name("label: \"\",")
                    .name("iconPath: \"")
                    .name(path == null ? "" : path)
                    .name("\"}")
                    .name(System.lineSeparator());

            sourceBuilder.token().keyword(SyntaxKind.ISOLATED_KEYWORD).keyword(SyntaxKind.FUNCTION_KEYWORD);
            sourceBuilder.token().name(toolName).keyword(SyntaxKind.OPEN_PAREN_TOKEN);
            sourceBuilder.token().name(String.join(", ", paramList));
            sourceBuilder.token().keyword(SyntaxKind.CLOSE_PAREN_TOKEN);

            Optional<Property> optReturnType = flowNode.getProperty(Property.TYPE_KEY);
            String returnType = "";
            if (optReturnType.isPresent()) {
                if (flowNode.getProperty(TARGET_TYPE).isPresent()) {
                    returnType = "json";
                } else {
                    returnType = optReturnType.get().value().toString();
                }
            }

            if (!returnType.isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURNS_KEYWORD)
                        .name(returnType);
                if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
                    sourceBuilder.token().keyword(SyntaxKind.PIPE_TOKEN).keyword(SyntaxKind.ERROR_KEYWORD);
                }
            }

            sourceBuilder.token().keyword(SyntaxKind.OPEN_BRACE_TOKEN);

            if (!returnType.isEmpty()) {
                sourceBuilder.token().expressionWithType(returnType,
                        flowNode.getProperty(Property.VARIABLE_KEY).orElseThrow()).keyword(SyntaxKind.EQUAL_TOKEN);
            }
            if (FlowNodeUtil.hasCheckKeyFlagSet(flowNode)) {
                sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
            }

            String resourcePath = flowNode.properties().get(Property.RESOURCE_PATH_KEY)
                    .codedata().originalName();

            if (resourcePath.equals(ParamUtils.REST_RESOURCE_PATH)) {
                resourcePath = flowNode.properties().get(Property.RESOURCE_PATH_KEY).value().toString();
            }

            for (String key : keys) {
                Optional<Property> property = flowNode.getProperty(key);
                if (property.isEmpty()) {
                    continue;
                }
                PropertyCodedata propCodedata = property.get()
                        .codedata();
                if (propCodedata == null) {
                    continue;
                }
                if (propCodedata.kind().equals(ParameterData.Kind.PATH_PARAM.name())) {
                    ignoredKeys.add(key);
                } else if (propCodedata.kind().equals(ParameterData.Kind.PATH_REST_PARAM.name())) {
                    String replacement = property.get().value().toString();
                    resourcePath = resourcePath.replace(ParamUtils.REST_PARAM_PATH, replacement);
                    ignoredKeys.add(key);
                }
            }

            sourceBuilder.token()
                    .name(connectionName)
                    .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                    .resourcePath(resourcePath)
                    .keyword(SyntaxKind.DOT_TOKEN)
                    .name(sourceBuilder.flowNode.codedata().symbol())
                    .stepOut()
                    .functionParameters(flowNode, ignoredKeys);

            if (!returnType.isEmpty()) {
                sourceBuilder.token()
                        .keyword(SyntaxKind.RETURN_KEYWORD)
                        .name(flowNode.getProperty(Property.VARIABLE_KEY).get().value().toString())
                        .endOfStatement();
            }
            sourceBuilder.token()
                    .keyword(SyntaxKind.CLOSE_BRACE_TOKEN);
            sourceBuilder.textEdit(false, AGENT_FILE);
            return gson.toJsonTree(sourceBuilder.build());
        }
        throw new IllegalStateException("Unsupported node kind to generate tool");
    }

    private boolean isToolAnnotated(FunctionSymbol functionSymbol) {
        for (AnnotationAttachmentSymbol annotAttachment : functionSymbol.annotAttachments()) {
            AnnotationSymbol annotationSymbol = annotAttachment.typeDescriptor();
            Optional<ModuleSymbol> optModule = annotationSymbol.getModule();
            if (optModule.isEmpty()) {
                continue;
            }
            ModuleID id = optModule.get().id();
            if (!(id.orgName().equals(BALLERINAX) && id.packageName().equals(AI_AGENT))) {
                continue;
            }
            Optional<String> optName = annotationSymbol.getName();
            if (optName.isEmpty()) {
                continue;
            }
            if (optName.get().equals(TOOL_ANNOTATION)) {
                return true;
            }
        }
        return false;
    }

    private boolean genDescription(String description, FlowNode flowNode, SourceBuilder sourceBuilder) {
        String desc = "";
        String flowNodeDesc = flowNode.metadata().description();
        if (!description.isEmpty()) {
            desc = description;
        } else if (flowNodeDesc != null && !flowNodeDesc.isEmpty()) {
            desc = flowNodeDesc;
        }
        boolean hasDescription = !desc.isEmpty();
        if (hasDescription) {
            sourceBuilder.token().descriptionDoc(desc);
        }
        return hasDescription;
    }

    public JsonArray getActions(JsonElement node, Path filePath, Project project, WorkspaceManager workspaceManager) {
        FlowNode flowNode = gson.fromJson(node, FlowNode.class);
        Document document = workspaceManager.document(filePath).orElseThrow();
        TextDocument textDocument = document.textDocument();
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode, workspaceManager, filePath);
        Path connectionPath = workspaceManager.projectRoot(filePath).resolve("connections.bal");
        List<TextEdit> connectionTextEdits = NodeBuilder.getNodeFromKind(flowNode.codedata().node())
                .toSource(sourceBuilder).get(connectionPath);
        io.ballerina.tools.text.TextEdit[] textEdits = new io.ballerina.tools.text.TextEdit[connectionTextEdits.size()];
        for (int i = 0; i < connectionTextEdits.size(); i++) {
            TextEdit connectionTextEdit = connectionTextEdits.get(i);
            Position start = connectionTextEdit.getRange().getStart();
            int startTextPosition = textDocument.textPositionFrom(LinePosition.from(start.getLine(),
                    start.getCharacter()));
            Position end = connectionTextEdit.getRange().getEnd();
            int endTextPosition = textDocument.textPositionFrom(LinePosition.from(end.getLine(), end.getCharacter()));
            io.ballerina.tools.text.TextEdit textEdit =
                    io.ballerina.tools.text.TextEdit.from(TextRange.from(startTextPosition,
                            endTextPosition - startTextPosition), connectionTextEdit.getNewText());
            textEdits[i] = textEdit;
        }
        TextDocument modifiedTextDoc = textDocument.apply(TextDocumentChange.from(textEdits));
        Document modifiedDoc =
                project.duplicate().currentPackage().module(document.module().moduleId())
                        .document(document.documentId()).modify().withContent(String.join(System.lineSeparator(),
                                modifiedTextDoc.textLines())).apply();

        SemanticModel newSemanticModel = modifiedDoc.module().packageInstance().getCompilation()
                .getSemanticModel(modifiedDoc.module().moduleId());
        Optional<Property> property = flowNode.getProperty(Property.VARIABLE_KEY);
        if (property.isEmpty()) {
            throw new IllegalStateException("Variable name is not present");
        }
        String variableName = property.get().value().toString();
        VariableSymbol variableSymbol = null;
        List<Symbol> moduleSymbols = newSemanticModel.moduleSymbols();
        for (Symbol moduleSymbol : moduleSymbols) {
            if (moduleSymbol.kind() != SymbolKind.VARIABLE) {
                continue;
            }
            if (moduleSymbol.getName().orElse("").equals(variableName)) {
                variableSymbol = (VariableSymbol) moduleSymbol;
            }
        }
        List<Item> methods = new ArrayList<>();
        if (variableSymbol == null) {
            return gson.toJsonTree(methods).getAsJsonArray();
        }

        // TODO: Derive this logic from AvailableNodeGenerator
        TypeReferenceTypeSymbol typeDescriptorSymbol =
                (TypeReferenceTypeSymbol) variableSymbol.typeDescriptor();
        ClassSymbol classSymbol = (ClassSymbol) typeDescriptorSymbol.typeDescriptor();
        if (!(classSymbol.qualifiers().contains(Qualifier.CLIENT))) {
            return gson.toJsonTree(methods).getAsJsonArray();
        }
        String parentSymbolName = variableSymbol.getName().orElseThrow();
        String className = classSymbol.getName().orElseThrow();

        // Obtain methods of the connector
        List<FunctionData> methodFunctionsData = new FunctionDataBuilder()
                .parentSymbol(classSymbol)
                .buildChildNodes();

        for (FunctionData methodFunction : methodFunctionsData) {
            String org = methodFunction.org();
            String packageName = methodFunction.packageName();
            String version = methodFunction.version();
            boolean isHttpModule = org.equals(BALLERINA_ORG) && packageName.equals(HTTP_MODULE);

            NodeBuilder nodeBuilder;
            String label;
            if (methodFunction.kind() == FunctionData.Kind.RESOURCE) {
                if (isHttpModule && HTTP_REMOTE_METHOD_SKIP_LIST.contains(methodFunction.name())) {
                    continue;
                }
                label = methodFunction.name() + (isHttpModule ? "" : methodFunction.resourcePath());
                nodeBuilder = NodeBuilder.getNodeFromKind(NodeKind.RESOURCE_ACTION_CALL);
            } else {
                label = methodFunction.name();
                nodeBuilder = switch (methodFunction.kind()) {
                    case REMOTE -> NodeBuilder.getNodeFromKind(NodeKind.REMOTE_ACTION_CALL);
                    case FUNCTION -> NodeBuilder.getNodeFromKind(NodeKind.METHOD_CALL);
                    default -> throw new IllegalStateException("Unexpected value: " + methodFunction.kind());
                };
            }

            Item item = nodeBuilder
                    .metadata()
                    .label(label)
                    .icon(CommonUtils.generateIcon(org, packageName, version))
                    .description(methodFunction.description())
                    .stepOut()
                    .codedata()
                    .org(org)
                    .module(packageName)
                    .object(className)
                    .symbol(methodFunction.name())
                    .version(version)
                    .parentSymbol(parentSymbolName)
                    .resourcePath(methodFunction.resourcePath())
                    .id(methodFunction.functionId())
                    .stepOut()
                    .buildAvailableNode();
            methods.add(item);
        }
        return gson.toJsonTree(methods).getAsJsonArray();
    }

    public JsonElement editTool(String toolName, String description, Path projectPath) {
        Map<Path, List<TextEdit>> textEditsMap = new HashMap<>();
        List<TextEdit> textEdits = new ArrayList<>();
        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.FUNCTION) {
                continue;
            }
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            if (!functionSymbol.getName().orElseThrow().equals(toolName)) {
                continue;
            }

            for (AnnotationAttachmentSymbol annotAttachment : functionSymbol.annotAttachments()) {
                AnnotationSymbol annotationSymbol = annotAttachment.typeDescriptor();
                if (!annotationSymbol.getName().orElseThrow().equals("Tool")) {
                    continue;
                }
                Location location = annotAttachment.getLocation().orElseThrow();
                textEdits.add(new TextEdit(CommonUtils.toRange(location.lineRange()),
                        "@agent:Tool {description: \"" + description + "\"}" + System.lineSeparator()));
                break;
            }
            textEditsMap.put(projectPath.resolve(functionSymbol.getLocation().orElseThrow().lineRange().fileName()),
                    textEdits);
            break;
        }
        return gson.toJsonTree(textEditsMap);
    }

    private Path addIsolateKeyword(String name, Path filePath, List<TextEdit> textEdits) {
        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.FUNCTION) {
                continue;
            }
            FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
            if (!functionSymbol.getName().orElseThrow().equals(name)) {
                continue;
            }
            if (functionSymbol.qualifiers().contains(Qualifier.ISOLATED)) {
                break;
            }
            Location location = functionSymbol.getLocation().orElseThrow();
            LineRange lineRange = location.lineRange();
            LinePosition startLine = lineRange.startLine();
            int offset = startLine.offset() - SyntaxKind.FUNCTION_KEYWORD.stringValue().length() - 1;
            int line = startLine.line();
            Position position = new Position(line, offset);
            textEdits.add(new TextEdit(new Range(position, position), "isolated "));
            Path parent = filePath.getParent();
            if (parent != null) {
                return parent.resolve(lineRange.fileName());
            }
            break;
        }
        return null;
    }
}
