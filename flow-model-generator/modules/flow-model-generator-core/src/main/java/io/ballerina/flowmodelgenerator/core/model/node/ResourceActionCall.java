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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.TypeUtils;
import io.ballerina.flowmodelgenerator.core.db.DatabaseManager;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import io.ballerina.flowmodelgenerator.core.utils.PackageUtil;
import io.ballerina.projects.Package;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the resource action invocation node in the flow model.
 *
 * @since 1.4.0
 */
public class ResourceActionCall extends NodeBuilder {

    public static final String TARGET_TYPE_KEY = "targetType";

    @Override
    public void setConcreteConstData() {
        codedata().node(NodeKind.RESOURCE_ACTION_CALL);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();
        FlowNode flowNode = sourceBuilder.flowNode;

        if (flowNode.properties().containsKey(Property.CHECK_ERROR_KEY) &&
                flowNode.properties().get(Property.CHECK_ERROR_KEY).value().equals(true)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        Optional<Property> connection = flowNode.getProperty(Property.CONNECTION_KEY);
        if (connection.isEmpty()) {
            throw new IllegalStateException("Client must be defined for an action call node");
        }
        return sourceBuilder.token()
                .name(connection.get().toSourceCode())
                .keyword(SyntaxKind.RIGHT_ARROW_TOKEN)
                .resourcePath(sourceBuilder.flowNode.properties().get(Property.RESOURCE_PATH_KEY).value().toString())
                .keyword(SyntaxKind.DOT_TOKEN)
                .name(sourceBuilder.flowNode.codedata().symbol())
                .stepOut()
                .functionParameters(flowNode,
                        Set.of(Property.CONNECTION_KEY, Property.VARIABLE_KEY,
                                Property.TYPE_KEY, TARGET_TYPE_KEY, Property.RESOURCE_PATH_KEY,
                                Property.CHECK_ERROR_KEY))
                .textEdit(false)
                .acceptImport()
                .build();
    }

    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        if (codedata.org().equals("$anon")) {
            return;
        }

        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> functionResult = codedata.id() != null ? dbManager.getFunction(codedata.id()) :
                dbManager.getAction(codedata.org(), codedata.module(), codedata.symbol(), codedata.resourcePath(),
                        DatabaseManager.FunctionKind.RESOURCE);
        if (functionResult.isEmpty()) {
            return;
        }

        FunctionResult function = functionResult.get();
        metadata()
                .label(function.name())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .org(function.org())
                .module(function.packageName())
                .object(NewConnection.CLIENT_SYMBOL)
                .id(function.functionId())
                .symbol(function.name());

        List<ParameterResult> functionParameters = dbManager.getFunctionParameters(function.functionId());
        for (ParameterResult paramResult : functionParameters) {
            if (paramResult.name().equals(TypeUtils.TARGET_TYPE)) {
                continue;
            }
            if (paramResult.kind() == ParameterKind.INCLUDED_RECORD) {
                Package modulePackage = PackageUtil
                        .getModulePackage(function.org(), function.packageName(), function.version());
                SemanticModel pkgModel = modulePackage.getDefaultModule().getCompilation().getSemanticModel();
                Optional<Symbol> includedRecordType = pkgModel.moduleSymbols().stream()
                        .filter(symbol -> symbol.nameEquals(paramResult.type().split(":")[1])).findFirst();
                if (includedRecordType.isPresent() && includedRecordType.get() instanceof TypeDefinitionSymbol) {
                    FunctionCall.addIncludedRecordToParams((
                            (TypeDefinitionSymbol) includedRecordType.get()).typeDescriptor(), this);
                }
            } else {
                properties().custom()
                        .metadata()
                            .label(paramResult.name())
                            .description(paramResult.description())
                            .stepOut()
                        .type(Property.ValueType.EXPRESSION)
                        .typeConstraint(paramResult.type())
                        .value(paramResult.getDefaultValue())
                        .editable()
                        .defaultable(paramResult.kind() == ParameterKind.DEFAULTABLE)
                        .stepOut()
                        .addProperty(paramResult.name());
            }
        }

        String returnTypeName = function.returnType();
        if (TypeUtils.hasReturn(function.returnType())) {
            boolean editable = false;
            if (returnTypeName.contains(ActionCall.TARGET_TYPE_KEY)) {
                returnTypeName = returnTypeName.replace(ActionCall.TARGET_TYPE_KEY, "json");
                editable = true;
            }
            properties()
                    .type(returnTypeName, editable)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
        }

        properties().custom()
                .metadata()
                .label(Property.CONNECTION_LABEL)
                .description(Property.CONNECTION_DOC)
                .stepOut()
                .typeConstraint(function.packageName() + ":" + NewConnection.CLIENT_SYMBOL)
                .value(codedata.parentSymbol())
                .stepOut()
                .addProperty(Property.CONNECTION_KEY);
        properties().resourcePath(function.resourcePath());

        if (function.returnError() == 1) {
            properties().checkError(true);
        }
    }
}
