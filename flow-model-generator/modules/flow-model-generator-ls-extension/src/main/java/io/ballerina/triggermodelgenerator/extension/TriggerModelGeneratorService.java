package io.ballerina.triggermodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.projects.Document;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import io.ballerina.triggermodelgenerator.extension.model.Codedata;
import io.ballerina.triggermodelgenerator.extension.model.Function;
import io.ballerina.triggermodelgenerator.extension.model.Parameter;
import io.ballerina.triggermodelgenerator.extension.model.Service;
import io.ballerina.triggermodelgenerator.extension.model.Trigger;
import io.ballerina.triggermodelgenerator.extension.model.TriggerBasicInfo;
import io.ballerina.triggermodelgenerator.extension.request.TriggerFunctionRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerSvcModelGenRequest;
import io.ballerina.triggermodelgenerator.extension.response.TriggerFunctionResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerResponse;
import io.ballerina.triggermodelgenerator.extension.response.TriggerSvcModelGenResponse;
import io.ballerina.triggermodelgenerator.extension.model.TriggerProperty;
import io.ballerina.triggermodelgenerator.extension.request.TriggerSourceGenRequest;
import io.ballerina.triggermodelgenerator.extension.response.TriggerSourceGenResponse;
import io.ballerina.triggermodelgenerator.extension.model.Value;
import io.ballerina.triggermodelgenerator.extension.request.TriggerListRequest;
import io.ballerina.triggermodelgenerator.extension.request.TriggerRequest;
import io.ballerina.triggermodelgenerator.extension.response.TriggerListResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Represents the extended language server service for the trigger model generator service.
 *
 * @since 1.4.0
 */
@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("triggerDesignService")
public class TriggerModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;
    private final Map<String, TriggerProperty> triggerProperties;

    public TriggerModelGeneratorService() {
        InputStream propertiesStream = getClass().getClassLoader()
                .getResourceAsStream("triggers/properties.json");
        Type mapType = new TypeToken<Map<String, TriggerProperty>>() {}.getType();
        Map<String, TriggerProperty> triggerProperties = Map.of();
        if (propertiesStream != null) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(propertiesStream, StandardCharsets.UTF_8))) {
                triggerProperties = new Gson().fromJson(reader, mapType);
            } catch (IOException e) {
                // Ignore
            }
        }
        this.triggerProperties = triggerProperties;
    }

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<TriggerListResponse> getTriggerModels(TriggerListRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            List<TriggerBasicInfo> triggerBasicInfoList = triggerProperties.values().stream()
                    .filter(triggerProperty -> filterTriggers(triggerProperty, request))
                    .map(trigger -> getTriggerBasicInfoByName(trigger.name()))
                    .flatMap(Optional::stream)
                    .toList();
            return new TriggerListResponse(triggerBasicInfoList);
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerResponse> getTriggerModel(TriggerRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (expectsTriggerByName(request)) {
                return new TriggerResponse(getTriggerByName(request.packageName()).orElse(null));
            }

            TriggerProperty triggerProperty = triggerProperties.get(request.id());
            if (triggerProperty == null) {
                return new TriggerResponse();
            }
            return new TriggerResponse(getTriggerByName(triggerProperty.name()).orElse(null));
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerSvcModelGenResponse> getTriggerModelFromCode(TriggerSvcModelGenRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerSvcModelGenResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.codedata().lineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof ServiceDeclarationNode serviceNode)) {
                    return new TriggerSvcModelGenResponse();
                }
                Optional<String> triggerName = getTriggerName(serviceNode);
                if (triggerName.isEmpty()) {
                    return new TriggerSvcModelGenResponse();
                }
                Optional<Service> service = getServiceFromTriggerName(triggerName.get());
                service.ifPresent(value -> updateTriggerServiceModel(value, serviceNode));
                return service.map(TriggerSvcModelGenResponse::new).orElseGet(TriggerSvcModelGenResponse::new);
            } catch (Throwable e) {
                return new TriggerSvcModelGenResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerSourceGenResponse> getSourceCode(TriggerSourceGenRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerSourceGenResponse();
                }
                ModulePartNode node = document.get().syntaxTree().rootNode();
                LineRange lineRange = node.lineRange();

                Trigger trigger = request.trigger();
                List<String> serviceDeclarations = new ArrayList<>();
                trigger.getServices().forEach(service -> {
                    if (service.isEnabled()) {
                        populateServiceProperties(service);
                        serviceDeclarations.add(getServiceDeclarationNode(trigger, service));
                    }
                });
                TextEdit serviceEdit = new TextEdit(CommonUtils.toRange(lineRange.endLine()),
                        System.lineSeparator() + String.join(System.lineSeparator(), serviceDeclarations));
                if (!importExists(node, trigger.getOrgName(), trigger.getModuleName())) {
                    String importText = String.format("%simport %s/%s;%s", System.lineSeparator(), trigger.getOrgName(),
                            trigger.getModuleName(), System.lineSeparator());
                    TextEdit importEdit = new TextEdit(CommonUtils.toRange(lineRange.startLine()), importText);
                    edits.add(importEdit);
                }
                edits.add(serviceEdit);
                return new TriggerSourceGenResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerSourceGenResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerFunctionResponse> addTriggerFunction(TriggerFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerFunctionResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.function().getCodedata().lineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof ServiceDeclarationNode serviceNode)) {
                    return new TriggerFunctionResponse();
                }
                LineRange functionLineRange = serviceNode.openBraceToken().lineRange();
                NodeList<Node> members = serviceNode.members();
                if (!members.isEmpty()) {
                    functionLineRange = members.get(members.size() - 1).lineRange();
                }
                String functionNode = "\n\t" + getFunction(request.function()).replace(System.lineSeparator(),
                        System.lineSeparator() + "\t");
                TextEdit functionEdit = new TextEdit(CommonUtils.toRange(functionLineRange.endLine()), functionNode);
                edits.add(functionEdit);
                return new TriggerFunctionResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerFunctionResponse(e);
            }
        });
    }

    @JsonRequest
    public CompletableFuture<TriggerFunctionResponse> updateTriggerFunction(TriggerFunctionRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextEdit> edits = new ArrayList<>();
                Path filePath = Path.of(request.filePath());
                Project project = this.workspaceManager.loadProject(filePath);
                Optional<Document> document = this.workspaceManager.document(filePath);
                if (document.isEmpty()) {
                    return new TriggerFunctionResponse();
                }
                SyntaxTree syntaxTree = document.get().syntaxTree();
                ModulePartNode modulePartNode = syntaxTree.rootNode();
                TextDocument textDocument = syntaxTree.textDocument();
                LineRange lineRange = request.function().getCodedata().lineRange();
                int start = textDocument.textPositionFrom(lineRange.startLine());
                int end = textDocument.textPositionFrom(lineRange.endLine());
                NonTerminalNode node = modulePartNode.findNode(TextRange.from(start, end - start), true);
                if (!(node instanceof FunctionDefinitionNode functionDefinitionNode)) {
                    return new TriggerFunctionResponse();
                }
                LineRange functionLineRange = functionDefinitionNode.functionSignature().lineRange();
                String functionSignature = getFunctionSignature(request.function());
                TextEdit functionEdit = new TextEdit(CommonUtils.toRange(functionLineRange), functionSignature);
                edits.add(functionEdit);
                return new TriggerFunctionResponse(Map.of(request.filePath(), edits));
            } catch (Throwable e) {
                return new TriggerFunctionResponse(e);
            }
        });
    }

    private void populateServiceProperties(Service service) {
        Value value = service.getProperty("requiredFunctions");
        if (Objects.nonNull(value) && value.isEnabled()) {
            String requiredFunction = value.getValue();
            service.getFunctions()
                    .forEach(function -> function.setEnabled(
                            function.getName().getValue().equals(requiredFunction)));
        }
    }

    private Optional<ExpressionNode> getListenerExpression(ServiceDeclarationNode serviceNode) {
        SeparatedNodeList<ExpressionNode> expressions = serviceNode.expressions();
        if (expressions.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode expressionNode = expressions.get(0);
        return Optional.of(expressionNode);
    }

    private Optional<String> getTriggerName(ServiceDeclarationNode serviceNode) {
        Optional<ExpressionNode> expressionNode = getListenerExpression(serviceNode);
        if (expressionNode.isEmpty()) {
            return Optional.empty();
        }
        if (!(expressionNode.get() instanceof ExplicitNewExpressionNode explicitNewExpressionNode)) {
            return Optional.empty();
        }
        TypeDescriptorNode typeDescriptorNode = explicitNewExpressionNode.typeDescriptor();
        if (!(typeDescriptorNode instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode)) {
            return Optional.empty();
        }
        return Optional.of(qualifiedNameReferenceNode.modulePrefix().text());
    }

    private Optional<Service> getServiceFromTriggerName(String name) {
        if (triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("triggers/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            Trigger trigger = new Gson().fromJson(reader, Trigger.class);
            Service service = trigger.getServices().get(0);
            service.setListener(trigger.getListener());

            return Optional.of(service);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Service getServiceModel(ServiceDeclarationNode serviceDeclarationNode) {
        Service serviceModel = Service.getNewService();
        serviceModel.setEnabled(true);
        Optional<TypeDescriptorNode> serviceTypeDesc = serviceDeclarationNode.typeDescriptor();
        if (serviceTypeDesc.isPresent()) {
            Value serviceType = serviceModel.getServiceType();
            serviceType.setValue(serviceTypeDesc.get().toString().trim());
            serviceType.setValueType("TYPE");
            serviceType.setEnabled(true);
        }
        NodeList<Node> paths = serviceDeclarationNode.absoluteResourcePath();
        if (!paths.isEmpty()) {
            Value basePath = serviceModel.getBasePath();
            basePath.setValue(getPath(paths));
            basePath.setValueType("EXPRESSION");
            basePath.setEnabled(true);
        }
        Optional<ExpressionNode> listenerExp = getListenerExpression(serviceDeclarationNode);
        if (listenerExp.isPresent()) {
            Value listener = serviceModel.getListener();
            listener.setEnabled(true);
            listener.setValue(listenerExp.get().toString().trim());
            listener.setValueType("EXPRESSION");
        }
        List<Function> functionModels = new ArrayList<>();
        serviceDeclarationNode.members().forEach(member -> {
            if (member instanceof FunctionDefinitionNode functionDefinitionNode) {
                Function functionModel = getFunctionModel(functionDefinitionNode);
                functionModels.add(functionModel);
            }
        });
        serviceModel.setFunctions(functionModels);
        return serviceModel;
    }

    private String getPath(NodeList<Node> paths) {
        return paths.stream().map(Node::toString).collect(Collectors.joining(""));
    }

    private Function getFunctionModel(FunctionDefinitionNode functionDefinitionNode) {
        Function functionModel = Function.getNewFunction();
        functionModel.setEnabled(true);
        Value functionName = functionModel.getName();
        functionName.setValue(functionDefinitionNode.functionName().text().trim());
        functionName.setValueType("IDENTIFIER");
        functionName.setEnabled(true);
        for (Token qualifier : functionDefinitionNode.qualifierList()) {
            if (qualifier.text().trim().matches("remote")) {
                functionModel.setKind("REMOTE");
            } else if (qualifier.text().trim().matches("resource")) {
                functionModel.setKind("RESOURCE");
            } else {
                functionModel.addQualifier(qualifier.text().trim());
            }
        }
        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        Optional<ReturnTypeDescriptorNode> returnTypeDesc = functionSignatureNode.returnTypeDesc();
        if (returnTypeDesc.isPresent()) {
            Value returnType = functionModel.getReturnType();
            returnType.setValue(returnTypeDesc.get().type().toString().trim());
            returnType.setValueType("TYPE");
            returnType.setEnabled(true);
        }
        SeparatedNodeList<ParameterNode> parameters = functionSignatureNode.parameters();
        List<Parameter> parameterModels = new ArrayList<>();
        parameters.forEach(parameterNode -> {
            Optional<Parameter> parameterModel = getParameterModel(parameterNode);
            parameterModel.ifPresent(parameterModels::add);
        });
        functionModel.setParameters(parameterModels);
        functionModel.setCodedata(new Codedata(functionDefinitionNode.lineRange()));
        return functionModel;
    }

    private Optional<Parameter> getParameterModel(ParameterNode parameterNode) {
        if (parameterNode instanceof RequiredParameterNode parameter) {
            Parameter parameterModel = Parameter.getNewParameter();
            parameterModel.setKind("REQUIRED");
            Value type = parameterModel.getType();
            type.setValue(parameter.typeName().toString().trim());
            type.setValueType("TYPE");
            type.setType(true);
            type.setEnabled(true);
            Value name = parameterModel.getName();
            name.setValue(parameter.paramName().get().toString().trim());
            name.setValueType("STRING");
            name.setEnabled(true);
            parameterModel.setEnabled(true);
            return Optional.of(parameterModel);
        }
        // Need to support other parameter types
        return Optional.empty();
    }

    private void updateTriggerServiceModel(Service serviceModel, ServiceDeclarationNode serviceNode) {
        Service commonSvcModel = getServiceModel(serviceNode);
        updateValue(serviceModel.getServiceType(), commonSvcModel.getServiceType());
        updateValue(serviceModel.getBasePath(), commonSvcModel.getBasePath());
        updateValue(serviceModel.getListener(), commonSvcModel.getListener());
        serviceModel.getFunctions().forEach(functionModel -> {
            Optional<Function> function = commonSvcModel.getFunctions().stream()
                    .filter(func -> func.getName().getValue().equals(functionModel.getName().getValue())
                            && func.getKind().equals(functionModel.getKind()))
                    .findFirst();
            function.ifPresentOrElse(
                    func -> updateFunction(functionModel, func),
                    () -> functionModel.setEnabled(false)
            );
        });
        List<Function> defaultFunctions = commonSvcModel.getFunctions().stream()
                .filter(func -> func.getKind().equals("DEFAULT")).toList();
        serviceModel.getFunctions().addAll(defaultFunctions);
    }

    private void updateValue(Value target, Value source) {
        if (Objects.isNull(target) || Objects.isNull(source)) {
            return;
        }
        target.setEnabled(source.isEnabled());
        target.setValue(source.getValue());
        target.setValueType(source.getValueType());
    }

    private void updateFunction(Function target, Function source) {
        target.setEnabled(source.isEnabled());
        target.setCodedata(source.getCodedata());
        updateValue(target.getAccessor(), source.getAccessor());
        updateValue(target.getName(), source.getName());
        target.getParameters().forEach(param -> {
            Optional<Parameter> parameter = source.getParameters().stream()
                    .filter(p -> p.getName().getValue().equals(param.getName().getValue()))
                    .findFirst();
            parameter.ifPresent(value -> updateParameter(param, value));
        });
        updateValue(target.getReturnType(), source.getReturnType());
    }

    private void updateParameter(Parameter target, Parameter source) {
        target.setEnabled(source.isEnabled());
        target.setKind(source.getKind());
        updateValue(target.getType(), source.getType());
        updateValue(target.getName(), source.getName());
    }

    private String getServiceDeclarationNode(Trigger trigger, Service service) {
        StringBuilder builder = new StringBuilder();
        builder.append("service ");
        if (Objects.nonNull(service.getServiceType()) && service.getServiceType().isEnabled()) {
            builder.append(getValueString(service.getServiceType()));
            builder.append(" ");
        }
        if (Objects.nonNull(service.getBasePath()) && service.getBasePath().isEnabled()) {
            builder.append(getValueString(service.getBasePath()));
            builder.append(" ");
        }
        builder.append("on ");
        Value listener = getListener(service, trigger);
        if (listener.isEnabled()) {
            builder.append(getValueString(listener));
        } else {
            builder.append("new ");
            builder.append(trigger.getListenerProtocol());
            builder.append(":Listener(");
            builder.append(getListenerParams(listener));
            builder.append(")");
        }
        builder.append(" {");
        builder.append(System.lineSeparator());
        List<String> functions = new ArrayList<>();
        service.getFunctions().forEach(function -> {
            if (function.isEnabled()) {
                String functionNode = "\t" + getFunction(function).replace(System.lineSeparator(),
                        System.lineSeparator() + "\t");
                functions.add(functionNode);
            }
        });
        builder.append(String.join(System.lineSeparator() + System.lineSeparator(), functions));
        builder.append(System.lineSeparator());
        builder.append("}");
        return builder.toString();
    }

    private Value getListener(Service service, Trigger trigger) {
        return Objects.nonNull(service.getListener()) ? service.getListener() : trigger.getListener();
    }

    private String getValueString(Value value) {
        if (!value.isEnabled()) {
           return "";
        }
        if (!value.getValue().trim().isEmpty()) {
            return value.getValueType().equals("STRING") ? String.format("\"%s\"", value.getValue()) : value.getValue();
        }
        if (!value.getPlaceholder().trim().isEmpty()) {
            return value.getValueType().equals("STRING") ? String.format("\"%s\"", value.getPlaceholder()) :
                    value.getPlaceholder();
        }
        Map<String, Value> properties = value.getProperties();
        if (Objects.isNull(properties)) {
            return "";
        }
        List<String> params = new ArrayList<>();
        properties.forEach((key, val) -> {
            if (val.isEnabled()) {
                params.add(String.format("%s: %s", key, getValueString(val)));
            }
        });
        return String.format("{%s}", String.join(", ", params));
    }

    private String getListenerParams(Value listener) {
        Map<String, Value> properties = listener.getProperties();
        List<String> params = new ArrayList<>();
        properties.forEach((key, value) -> {
            if (value.isEnabled()) {
                params.add(String.format("%s = %s", key, getValueString(value)));
            }
        });
        return String.join(", ", params);
    }

    private String getFunction(Function function) {
        StringBuilder builder = new StringBuilder();
        String functionQualifiers = getFunctionQualifiers(function);
        if (!functionQualifiers.isEmpty()) {
            builder.append(functionQualifiers);
            builder.append(" ");
        }
        builder.append("function ");
        if (function.getKind().equals("RESOURCE") && Objects.nonNull(function.getAccessor())
                && function.getAccessor().isEnabled()) {
            builder.append(getValueString(function.getAccessor()));
            builder.append(" ");
        }
        builder.append(getValueString(function.getName()));
        builder.append(getFunctionSignature(function));
        builder.append("{");
        builder.append(System.lineSeparator());
        builder.append("\tdo {");
        builder.append(System.lineSeparator());
        builder.append("\t} on fail error err {");
        builder.append(System.lineSeparator());
        builder.append("\t\t// handle error");
        builder.append(System.lineSeparator());
        builder.append("\t}");
        builder.append(System.lineSeparator());
        builder.append("}");
        return builder.toString();
    }

    private String getFunctionSignature(Function function) {
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        List<String> params = new ArrayList<>();
        function.getParameters().forEach(param -> {
            if (param.isEnabled()) {
                params.add(String.format("%s %s", getValueString(param.getType()), getValueString(param.getName())));
            }
        });
        builder.append(String.join(", ", params));
        builder.append(")");
        if (function.getReturnType().isEnabled()) {
            builder.append(" returns ");
            builder.append(getValueString(function.getReturnType()));
        }
        builder.append(" ");
        return builder.toString();
    }

    private String getFunctionQualifiers(Function function) {
        List<String> qualifiers = function.getQualifiers();
        qualifiers = Objects.isNull(qualifiers) ? new ArrayList<>() : qualifiers;
        String kind = function.getKind();
        switch (kind) {
            case "REMOTE" -> qualifiers.add("remote");
            case "RESOURCE" -> qualifiers.add("resource");
        }
        return String.join(" ", qualifiers);
    }

    private boolean importExists(ModulePartNode node, String org, String module) {
        return node.imports().stream().anyMatch(importDeclarationNode -> {
            String moduleName = importDeclarationNode.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            return importDeclarationNode.orgName().isPresent() &&
                    org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                    module.equals(moduleName);
        });
    }

    private boolean filterTriggers(TriggerProperty triggerProperty, TriggerListRequest request) {
        return (request == null) || ((request.organization() == null || request.organization().equals(triggerProperty.orgName())) &&
                (request.packageName() == null || request.packageName().equals(triggerProperty.packageName())) &&
                (request.keyWord() == null || triggerProperty.keywords().stream()
                        .anyMatch(keyword -> keyword.equalsIgnoreCase(request.keyWord()))) &&
                (request.query() == null || triggerProperty.keywords().stream()
                        .anyMatch(keyword -> keyword.contains(request.query()))));
    }

    private static boolean expectsTriggerByName(TriggerRequest request) {
        return request.id() == null && request.organization() != null && request.packageName() != null;
    }

    private Optional<TriggerBasicInfo> getTriggerBasicInfoByName(String name) {
        if (triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("triggers/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, TriggerBasicInfo.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<Trigger> getTriggerByName(String name) {
        if (triggerProperties.values().stream().noneMatch(trigger -> trigger.name().equals(name))) {
            return Optional.empty();
        }
        InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(String.format("triggers/%s.json", name));
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Trigger.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
