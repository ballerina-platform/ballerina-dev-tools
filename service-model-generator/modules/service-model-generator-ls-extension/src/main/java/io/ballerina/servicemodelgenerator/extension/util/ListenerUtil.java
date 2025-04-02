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

package io.ballerina.servicemodelgenerator.extension.util;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.DisplayAnnotation;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.servicemodelgenerator.extension.util.Utils.removeLeadingSingleQuote;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.upperCaseFirstLetter;

/**
 * Util class for Listener related operations.
 *
 * @since 2.0.0
 */
public class ListenerUtil {

    public static Set<String> getCompatibleListeners(String moduleName, SemanticModel semanticModel, Project project) {
        Set<String> listeners = new LinkedHashSet<>();
        boolean isHttpDefaultListenerDefined = false;
        boolean isHttp = ServiceModelGeneratorConstants.HTTP.equals(moduleName);
        for (Symbol moduleSymbol : semanticModel.moduleSymbols()) {
            if (!(moduleSymbol instanceof VariableSymbol variableSymbol)
                    || !variableSymbol.qualifiers().contains(Qualifier.LISTENER)) {
                continue;
            }
            Optional<ModuleSymbol> module = variableSymbol.typeDescriptor().getModule();
            if (module.isEmpty() || !module.get().id().moduleName().equals(moduleName) ||
                    variableSymbol.getName().isEmpty()) {
                continue;
            }
            String listenerName = variableSymbol.getName().get();
            if (isHttp) {
                if (variableSymbol.getLocation().isPresent()) {
                    Location location = variableSymbol.getLocation().get();
                    Path path = project.sourceRoot().resolve(location.lineRange().fileName());
                    DocumentId documentId = project.documentId(path);
                    Document document = project.currentPackage().getDefaultModule().document(documentId);
                    if (document != null) {
                        ModulePartNode node = document.syntaxTree().rootNode();
                        TextRange range = TextRange.from(location.textRange().startOffset(),
                                location.textRange().length());
                        NonTerminalNode foundNode = node.findNode(range);
                        if (foundNode != null) {
                            while (foundNode != null && !(foundNode instanceof ListenerDeclarationNode)) {
                                foundNode = foundNode.parent();
                            }
                            if (foundNode != null) {
                                ListenerDeclarationNode listenerDeclarationNode = (ListenerDeclarationNode) foundNode;
                                isHttpDefaultListenerDefined = listenerDeclarationNode.initializer().toSourceCode()
                                        .trim().contains("http:getDefaultListener()");
                            }
                        }
                    }
                }
            }
            listeners.add(listenerName);
        }

        if (isHttp && !isHttpDefaultListenerDefined) {
            listeners.add(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_ITEM_LABEL);
        }

        return listeners;
    }

    public static Optional<String> getHttpDefaultListenerNameRef(SemanticModel semanticModel, Project project) {
        for (Symbol moduleSymbol : semanticModel.moduleSymbols()) {
            if (!(moduleSymbol instanceof VariableSymbol variableSymbol)
                    || !variableSymbol.qualifiers().contains(Qualifier.LISTENER)) {
                continue;
            }
            Optional<ModuleSymbol> module = variableSymbol.typeDescriptor().getModule();
            if (module.isEmpty() || !module.get().id().moduleName().equals("http") ||
                    variableSymbol.getName().isEmpty()) {
                continue;
            }
            String listenerName = variableSymbol.getName().get().trim();
            if (variableSymbol.getLocation().isPresent()) {
                Location location = variableSymbol.getLocation().get();
                Path path = project.sourceRoot().resolve(location.lineRange().fileName());
                DocumentId documentId = project.documentId(path);
                Document document = project.currentPackage().getDefaultModule().document(documentId);
                if (document != null) {
                    ModulePartNode node = document.syntaxTree().rootNode();
                    TextRange range = TextRange.from(location.textRange().startOffset(),
                            location.textRange().length());
                    NonTerminalNode foundNode = node.findNode(range);
                    if (foundNode != null) {
                        while (foundNode != null && !(foundNode instanceof ListenerDeclarationNode)) {
                            foundNode = foundNode.parent();
                        }
                        if (foundNode != null) {
                            ListenerDeclarationNode listenerDeclarationNode = (ListenerDeclarationNode) foundNode;
                            boolean found = listenerDeclarationNode.initializer().toSourceCode()
                                    .trim().contains("http:getDefaultListener()");
                            if (found) {
                                return Optional.of(listenerName);
                            }
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    public static boolean createDefaultListener(Value listener) {
        if (Objects.nonNull(listener) && listener.isEnabledWithValue()) {
            List<String> values = listener.getValues();
            if (Objects.nonNull(values) && !values.isEmpty()) {
                List<String> valuesList = new ArrayList<>() {{
                    addAll(values);
                }};
                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i).equals(
                            ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_ITEM_LABEL)) {
                        valuesList.set(i, ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_VAR_NAME);
                        listener.setValues(valuesList);
                        return true;
                    }
                }
            } else {
                if (listener.getValue().equals(
                        ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_ITEM_LABEL)) {
                    listener.setValue(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_VAR_NAME);
                    return true;
                }
            }
        }
        return false;
    }

    public static String getHttpDefaultListenerDeclarationStmt(SemanticModel semanticModel, Document document,
                                                               LinePosition linePosition) {
        String variableName = Utils.generateVariableIdentifier(semanticModel, document, linePosition,
                ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_VAR_NAME);
        return String.format(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_STMT, variableName);
    }

    public static Listener getListenerModelWithoutParamProps(FunctionData functionData) {
        Map<String, Value> properties = new LinkedHashMap<>();
        String formattedModuleName = upperCaseFirstLetter(functionData.packageName());
        String icon = CommonUtils.generateIcon(functionData.org(), functionData.packageName(),
                functionData.version());
        Listener.ListenerBuilder listenerBuilder = new Listener.ListenerBuilder();
        listenerBuilder
                .setId(functionData.packageId())
                .setName(formattedModuleName + " Listener")
                .setType(functionData.packageName())
                .setDisplayName(formattedModuleName)
                .setDescription(functionData.description())
                .setListenerProtocol(getListenerProtocol(functionData.packageName()))
                .setModuleName(functionData.packageName())
                .setOrgName(functionData.org())
                .setPackageName(functionData.packageName())
                .setVersion(functionData.version())
                .setIcon(icon)
                .setDisplayAnnotation(new DisplayAnnotation(formattedModuleName, icon))
                .setProperties(properties);

        properties.put("name", nameProperty());
        return listenerBuilder.build();
    }

    private static String getListenerProtocol(String packageName) {
        String pkgName = packageName.toLowerCase(Locale.ROOT);
        String[] split = pkgName.split("\\.");
        return split[split.length - 1];
    }

    public static Optional<Listener> getListenerModelByName(String moduleName) {
        ServiceDatabaseManager dbManager = ServiceDatabaseManager.getInstance();
        Optional<FunctionData> optFunctionResult = dbManager.getListener(moduleName);
        if (optFunctionResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionData functionData = optFunctionResult.get();
        LinkedHashMap<String, ParameterData> parameters = dbManager
                .getFunctionParametersAsMap(functionData.functionId());
        functionData.setParameters(parameters);

        Listener listener = getListenerModelWithoutParamProps(functionData);
        setParameterProperties(functionData, listener.getProperties());
        return Optional.of(listener);
    }

    public static Optional<Listener> getDefaultListenerModel(ListenerDeclarationNode listenerNode) {
        ServiceDatabaseManager dbManager = ServiceDatabaseManager.getInstance();
        Optional<FunctionData> optFunctionResult = dbManager.getListener("http");
        if (optFunctionResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionData functionData = optFunctionResult.get();
        LinkedHashMap<String, ParameterData> parameters = dbManager
                .getFunctionParametersAsMap(functionData.functionId());
        functionData.setParameters(parameters);
        Listener listener = getListenerModelWithoutParamProps(functionData);
        listener.getProperties().put("defaultListener", getHttpDefaultListenerValue());
        Value nameProperty = listener.getProperty("name");
        nameProperty.setValue(listenerNode.variableName().text().trim());
        nameProperty.setCodedata(new Codedata(listenerNode.variableName().lineRange()));
        nameProperty.setEditable(false);
        listener.setCodedata(new Codedata(listenerNode.lineRange()));
        return Optional.of(listener);
    }

    private static Value getHttpDefaultListenerValue() {
        Value value = new Value();
        value.setMetadata(new MetaData("HTTP Default Listener",
                "The default HTTP listener"));
        value.setEnabled(true);
        value.setEditable(false);
        value.setAdvanced(false);
        value.setOptional(false);
        value.setValueType(ServiceModelGeneratorConstants.VALUE_TYPE_EXPRESSION);
        value.setValue(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_EXPR);
        return value;
    }

    private static void setParameterProperties(FunctionData function, Map<String, Value> properties) {
        for (ParameterData paramResult : function.parameters().values()) {
            if (paramResult.kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = removeLeadingSingleQuote(paramResult.name());

            Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
            codedata.setOriginalName(paramResult.name());

            Value.ValueBuilder valueBuilder = new Value.ValueBuilder()
                    .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                    .setCodedata(codedata)
                    .value("")
                    .valueType("EXPRESSION")
                    .setPlaceholder(paramResult.defaultValue())
                    .setValueTypeConstraint(paramResult.type())
                    .editable(true)
                    .setType(false)
                    .enabled(true)
                    .setOptional(paramResult.optional())
                    .setAdvanced(paramResult.optional())
                    .setTypeMembers(paramResult.typeMembers());

            properties.put(unescapedParamName, valueBuilder.build());
        }
    }

    public static Optional<Listener> getListenerFromSource(ListenerDeclarationNode listenerDeclarationNode,
                                                           SemanticModel semanticModel) {
        if (ListenerUtil.isHttpDefaultListener(listenerDeclarationNode)) {
            return ListenerUtil.getDefaultListenerModel(listenerDeclarationNode);
        }
        Optional<Symbol> symbol = semanticModel.symbol(listenerDeclarationNode.typeDescriptor().get());
        if (symbol.isEmpty() || !(symbol.get() instanceof TypeSymbol typeSymbol) || typeSymbol.getModule().isEmpty()) {
            return Optional.empty();
        }

        String moduleName = typeSymbol.getModule().get().id().moduleName();
        ServiceDatabaseManager dbManager = ServiceDatabaseManager.getInstance();
        Optional<FunctionData> optFunctionResult = dbManager.getListener(moduleName);
        if (optFunctionResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionData functionData = optFunctionResult.get();
        LinkedHashMap<String, ParameterData> parameters = dbManager
                .getFunctionParametersAsMap(functionData.functionId());
        functionData.setParameters(parameters);

        Listener listener = getListenerModelWithoutParamProps(functionData);
        Value nameProperty = listener.getProperty("name");
        nameProperty.setValue(listenerDeclarationNode.variableName().text().trim());
        nameProperty.setCodedata(new Codedata(listenerDeclarationNode.variableName().lineRange()));
        nameProperty.setEditable(false);
        listener.setCodedata(new Codedata(listenerDeclarationNode.lineRange()));
        Node initializer = listenerDeclarationNode.initializer();
        if (initializer instanceof NewExpressionNode newExpressionNode) {
            TypeSymbol rawType = CommonUtils.getRawType(typeSymbol);
            if (rawType instanceof ClassSymbol classSymbol) {
                SeparatedNodeList<FunctionArgumentNode> arguments = getArgList(newExpressionNode);
                if (classSymbol.initMethod().isEmpty()) {
                    return Optional.of(listener);
                }
                ListenerDeclAnalyzer analyzer = new ListenerDeclAnalyzer(listener.getProperties());
                analyzer.analyze(arguments, classSymbol.initMethod().get(), functionData);
            }
        }
        return Optional.of(listener);
    }

    private static SeparatedNodeList<FunctionArgumentNode> getArgList(NewExpressionNode newExpressionNode) {
        if (newExpressionNode instanceof ExplicitNewExpressionNode explicitNewExpressionNode) {
            return explicitNewExpressionNode.parenthesizedArgList().arguments();
        } else {
            Optional<ParenthesizedArgList> parenthesizedArgList = ((ImplicitNewExpressionNode) newExpressionNode)
                    .parenthesizedArgList();
            return parenthesizedArgList.isPresent() ? parenthesizedArgList.get().arguments() :
                    NodeFactory.createSeparatedNodeList();
        }
    }

    public static boolean isHttpDefaultListener(ListenerDeclarationNode listenerNode) {
        return listenerNode.initializer().toSourceCode().trim().contains(ServiceModelGeneratorConstants
                .HTTP_DEFAULT_LISTENER_EXPR);
    }

    public static Value nameProperty() {

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData("Name", "The name of the listener"))
                .setCodedata(new Codedata("LISTENER_VAR_NAME"))
                .value("")
                .valueType("IDENTIFIER")
                .setValueTypeConstraint("string")
                .setType(false)
                .editable(true)
                .enabled(true)
                .setOptional(false)
                .setAdvanced(false);

        return valueBuilder.build();
    }
}
