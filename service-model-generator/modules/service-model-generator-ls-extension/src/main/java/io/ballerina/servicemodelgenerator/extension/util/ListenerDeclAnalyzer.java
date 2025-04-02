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

import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.ParameterData;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import static io.ballerina.servicemodelgenerator.extension.util.Utils.removeLeadingSingleQuote;

/**
 * Analyzes the Listener declaration and generates the properties.
 *
 * @since 2.0.0
 */
public class ListenerDeclAnalyzer {

    private final Map<String, Value> properties;

    public ListenerDeclAnalyzer(Map<String, Value> properties) {
        this.properties = properties;
    }

    public Map<String, Value> getProperties() {
        return properties;
    }

    public void analyze(SeparatedNodeList<FunctionArgumentNode> arguments,
                        FunctionSymbol functionSymbol, FunctionData functionData) {
        final Map<String, Node> namedArgValueMap = new HashMap<>();
        final Queue<Node> positionalArgs = new LinkedList<>();
        calculateFunctionArgs(namedArgValueMap, positionalArgs, arguments);
        buildPropsFromFuncCallArgs(arguments, functionSymbol.typeDescriptor(), functionData.parameters(),
                positionalArgs, namedArgValueMap);
    }

    private void calculateFunctionArgs(Map<String, Node> namedArgValueMap,
                                       Queue<Node> positionalArgs,
                                       SeparatedNodeList<FunctionArgumentNode> argumentNodes) {
        if (argumentNodes != null) {
            for (FunctionArgumentNode argument : argumentNodes) {
                switch (argument.kind()) {
                    case NAMED_ARG -> {
                        NamedArgumentNode namedArgument = (NamedArgumentNode) argument;
                        namedArgValueMap.put(namedArgument.argumentName().name().text(),
                                namedArgument.expression());
                    }
                    case POSITIONAL_ARG -> positionalArgs.add(((PositionalArgumentNode) argument).expression());
                    default -> {
                        // Ignore the default case
                    }
                }
            }
        }
    }

    private void buildPropsFromFuncCallArgs(SeparatedNodeList<FunctionArgumentNode> argumentNodes,
                                            FunctionTypeSymbol functionTypeSymbol,
                                            Map<String, ParameterData> funcParamMap,
                                            Queue<Node> positionalArgs, Map<String, Node> namedArgValueMap) {
        if (argumentNodes == null) { // new;
            List<ParameterData> functionParameters = funcParamMap.values().stream().toList();
            for (ParameterData paramResult : functionParameters) {
                ParameterData.Kind paramKind = paramResult.kind();

                if (paramKind.equals(ParameterData.Kind.PATH_PARAM) ||
                        paramKind.equals(ParameterData.Kind.PATH_REST_PARAM)
                        || paramKind.equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                        || paramKind.equals(ParameterData.Kind.INCLUDED_RECORD)) {
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
                        .isType(false)
                        .enabled(true)
                        .setOptional(paramResult.optional())
                        .setAdvanced(paramResult.optional())
                        .setTypeMembers(paramResult.typeMembers());
                properties.put(unescapedParamName, valueBuilder.build());
            }
            return;
        }

        if (functionTypeSymbol.restParam().isPresent()) {
            ParameterSymbol restParamSymbol = functionTypeSymbol.restParam().get();
            Optional<List<ParameterSymbol>> paramsOptional = functionTypeSymbol.params();

            if (paramsOptional.isPresent()) {
                List<ParameterSymbol> paramsList = paramsOptional.get();
                int paramCount = paramsList.size(); // param count without rest params
                int argCount = positionalArgs.size();

                List<String> restArgs = new ArrayList<>();
                for (int i = 0; i < paramsList.size(); i++) {
                    ParameterSymbol parameterSymbol = paramsList.get(i);
                    String escapedParamName = parameterSymbol.getName().get();
                    ParameterData paramResult = funcParamMap.get(escapedParamName);
                    if (paramResult == null) {
                        escapedParamName = CommonUtil.escapeReservedKeyword(parameterSymbol.getName().get());
                    }
                    paramResult = funcParamMap.get(escapedParamName);
                    Node paramValue = i < argCount ? positionalArgs.poll()
                            : namedArgValueMap.get(paramResult.name());

                    funcParamMap.remove(parameterSymbol.getName().get());
                    String value = paramValue != null ? paramValue.toSourceCode() : null;
                    String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
                    Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                    codedata.setOriginalName(paramResult.name());
                    Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                    valueBuilder
                            .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                            .setCodedata(codedata)
                            .value(value)
                            .valueType("EXPRESSION")
                            .setPlaceholder(paramResult.defaultValue())
                            .setValueTypeConstraint(paramResult.type())
                            .editable(true)
                            .isType(false)
                            .enabled(true)
                            .setOptional(paramResult.optional())
                            .setAdvanced(paramResult.optional())
                            .setTypeMembers(paramResult.typeMembers());
                    properties.put(unescapedParamName, valueBuilder.build());
                }

                for (int i = paramCount; i < argCount; i++) {
                    restArgs.add(Objects.requireNonNull(positionalArgs.poll()).toSourceCode());
                }

                String escapedParamName = CommonUtil.escapeReservedKeyword(restParamSymbol.getName().get());
                ParameterData restParamResult = funcParamMap.get(escapedParamName);
                funcParamMap.remove(restParamSymbol.getName().get());
                String unescapedParamName = removeLeadingSingleQuote(restParamResult.name());

                Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                codedata.setOriginalName(restParamResult.name());

                Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                valueBuilder
                        .setMetadata(new MetaData(unescapedParamName, restParamResult.description()))
                        .setCodedata(codedata)
                        .value("[%s]".formatted(String.join(", ", restArgs)))
                        .valueType("EXPRESSION")
                        .setPlaceholder(restParamResult.defaultValue())
                        .setValueTypeConstraint(restParamResult.type())
                        .editable(true)
                        .isType(false)
                        .enabled(true)
                        .setOptional(restParamResult.optional())
                        .setAdvanced(restParamResult.optional())
                        .setTypeMembers(restParamResult.typeMembers());

                properties.put(unescapedParamName, valueBuilder.build());
            }
            // iterate over functionParamMap
            addRemainingParamsToPropertyMap(funcParamMap);
            return;
        }
        Optional<List<ParameterSymbol>> paramsOptional = functionTypeSymbol.params();
        if (paramsOptional.isPresent()) {
            List<ParameterSymbol> paramsList = paramsOptional.get();
            int argCount = positionalArgs.size();

            for (int i = 0; i < paramsList.size(); i++) {
                ParameterSymbol parameterSymbol = paramsList.get(i);
                String escapedParamName = parameterSymbol.getName().get();
                ParameterData paramResult = funcParamMap.get(escapedParamName);
                if (paramResult == null) {
                    escapedParamName = CommonUtil.escapeReservedKeyword(parameterSymbol.getName().get());
                }
                paramResult = funcParamMap.get(escapedParamName);
                Node paramValue;
                if (i < argCount) {
                    paramValue = positionalArgs.poll();
                } else {
                    paramValue = namedArgValueMap.get(paramResult.name());
                    namedArgValueMap.remove(paramResult.name());
                }
                if (paramResult.kind() == ParameterData.Kind.INCLUDED_RECORD) {
                    if (argumentNodes.size() > i && argumentNodes.get(i).kind() == SyntaxKind.NAMED_ARG) {
                        FunctionArgumentNode argNode = argumentNodes.get(i);
                        funcParamMap.remove(escapedParamName);
                        NamedArgumentNode namedArgumentNode = (NamedArgumentNode) argNode;
                        String argName = namedArgumentNode.argumentName().name().text();
                        if (argName.equals(paramResult.name())) {  // foo("a", b = {})
                            paramResult = funcParamMap.get(escapedParamName);
                            String value = paramValue != null ? paramValue.toSourceCode() : null;
                            String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
                            Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                            codedata.setOriginalName(paramResult.name());

                            Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                            valueBuilder
                                    .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                                    .setCodedata(codedata)
                                    .value(value)
                                    .valueType("EXPRESSION")
                                    .setPlaceholder(paramResult.defaultValue())
                                    .setValueTypeConstraint(paramResult.type())
                                    .editable(true)
                                    .isType(false)
                                    .enabled(true)
                                    .setOptional(paramResult.optional())
                                    .setAdvanced(paramResult.optional())
                                    .setTypeMembers(paramResult.typeMembers());

                            properties.put(unescapedParamName, valueBuilder.build());
                        } else {
                            if (funcParamMap.containsKey(argName)) { // included record attribute
                                paramResult = funcParamMap.get(argName);
                                funcParamMap.remove(argName);
                                if (paramValue == null) {
                                    paramValue = namedArgValueMap.get(argName);
                                    namedArgValueMap.remove(argName);
                                }
                                String value = paramValue != null ? paramValue.toSourceCode() : "";
                                String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
                                Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                                codedata.setOriginalName(paramResult.name());

                                Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                                valueBuilder
                                        .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                                        .setCodedata(codedata)
                                        .value(value)
                                        .valueType("EXPRESSION")
                                        .setPlaceholder(paramResult.defaultValue())
                                        .setValueTypeConstraint(paramResult.type())
                                        .editable(true)
                                        .isType(false)
                                        .enabled(true)
                                        .setOptional(paramResult.optional())
                                        .setAdvanced(paramResult.optional())
                                        .setTypeMembers(paramResult.typeMembers());

                                properties.put(unescapedParamName, valueBuilder.build());

                            }
                        }

                    } else { // positional arg
                        if (paramValue != null) {
                            String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
                            funcParamMap.remove(escapedParamName);
                            String value = paramValue.toSourceCode();
                            Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                            codedata.setOriginalName(paramResult.name());

                            Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                            valueBuilder
                                    .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                                    .setCodedata(codedata)
                                    .value(value)
                                    .valueType("EXPRESSION")
                                    .setPlaceholder(paramResult.defaultValue())
                                    .setValueTypeConstraint(paramResult.type())
                                    .editable(true)
                                    .isType(false)
                                    .enabled(true)
                                    .setOptional(paramResult.optional())
                                    .setAdvanced(paramResult.optional())
                                    .setTypeMembers(paramResult.typeMembers());

                            properties.put(unescapedParamName, valueBuilder.build());
                            return;
                        }
                    }
                }

                if (paramValue == null && paramResult.kind() == ParameterData.Kind.INCLUDED_RECORD) {
                    funcParamMap.remove(escapedParamName);
                    continue;
                }
                funcParamMap.remove(escapedParamName);
                String value = paramValue != null ? paramValue.toSourceCode() : "";
                String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
                Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                codedata.setOriginalName(paramResult.name());

                Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                valueBuilder
                        .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                        .setCodedata(codedata)
                        .value(value)
                        .valueType("EXPRESSION")
                        .setPlaceholder(paramResult.defaultValue())
                        .setValueTypeConstraint(paramResult.type())
                        .editable(true)
                        .isType(false)
                        .enabled(true)
                        .setOptional(paramResult.optional())
                        .setAdvanced(paramResult.optional())
                        .setTypeMembers(paramResult.typeMembers());
                properties.put(unescapedParamName, valueBuilder.build());
            }

            for (Map.Entry<String, Node> entry : namedArgValueMap.entrySet()) { // handle remaining named args
                String escapedParamName = CommonUtil.escapeReservedKeyword(entry.getKey());
                if (!funcParamMap.containsKey(escapedParamName)) {
                    continue;
                }
                ParameterData paramResult = funcParamMap.remove(escapedParamName);
                String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
                Node paramValue = entry.getValue();
                String value = paramValue != null ? paramValue.toSourceCode() : "";
                Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
                Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
                codedata.setOriginalName(paramResult.name());
                valueBuilder
                        .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                        .setCodedata(codedata)
                        .value(value)
                        .valueType("EXPRESSION")
                        .setPlaceholder(paramResult.defaultValue())
                        .setValueTypeConstraint(paramResult.type())
                        .editable(true)
                        .isType(false)
                        .enabled(true)
                        .setOptional(paramResult.optional())
                        .setAdvanced(paramResult.optional())
                        .setTypeMembers(paramResult.typeMembers());
                properties.put(unescapedParamName, valueBuilder.build());
            }
            addRemainingParamsToPropertyMap(funcParamMap);
        }
    }

    private void addRemainingParamsToPropertyMap(Map<String, ParameterData> funcParamMap) {
        for (Map.Entry<String, ParameterData> entry : funcParamMap.entrySet()) {
            ParameterData paramResult = entry.getValue();
            if (paramResult.kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = removeLeadingSingleQuote(paramResult.name());
            Codedata codedata = new Codedata("LISTENER_INIT_PARAM");
            codedata.setOriginalName(paramResult.name());
            Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
            valueBuilder
                    .setMetadata(new MetaData(unescapedParamName, paramResult.description()))
                    .setCodedata(codedata)
                    .value("")
                    .valueType("EXPRESSION")
                    .setPlaceholder(paramResult.defaultValue())
                    .setValueTypeConstraint(paramResult.type())
                    .editable(true)
                    .isType(false)
                    .enabled(true)
                    .setOptional(paramResult.optional())
                    .setAdvanced(paramResult.optional())
                    .setTypeMembers(paramResult.typeMembers());
            properties.put(unescapedParamName, valueBuilder.build());
        }
    }
}

