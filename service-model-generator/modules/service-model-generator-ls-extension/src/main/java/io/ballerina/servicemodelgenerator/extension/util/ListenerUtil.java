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
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Project;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.NameUtil;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                    if (isDefaultListenerRef(variableSymbol, project)) {
                        isHttpDefaultListenerDefined = true;
                        listenerName = ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER;
                    }
                }
            }
            listeners.add(listenerName);
        }

        if (isHttp && !isHttpDefaultListenerDefined) {
            listeners.add(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER);
        }

        return listeners;
    }

    public static boolean isDefaultListenerRef(Symbol symbol, Project project) {
        Location location = symbol.getLocation().get();
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
                    return listenerDeclarationNode.initializer().toSourceCode()
                            .trim().contains("http:getDefaultListener()");
                }
            }
        }
        return false;
    }

    public static boolean checkForDefaultListenerExistence(Value listener) {
        if (Objects.nonNull(listener) && listener.isEnabledWithValue()) {
            List<String> values = listener.getValues();
            if (Objects.nonNull(values) && !values.isEmpty()) {
                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i).equals(
                            ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER)) {
                        values.set(i, ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_VAR_NAME);
                        return true;
                    }
                }
            } else {
                if (listener.getValue().equals(
                        ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER)) {
                    listener.setValue(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_VAR_NAME);
                    return true;
                }
            }
        }
        return false;
    }

    public static String getListenerDeclarationStmt(SemanticModel semanticModel, Document document,
                                                    LinePosition linePosition) {
        Set<String> names = semanticModel.visibleSymbols(document, linePosition).parallelStream()
                .filter(s -> s.getName().isPresent())
                .map(s -> s.getName().get())
                .collect(Collectors.toSet());
        String variableName = NameUtil.generateVariableName(
                ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_VAR_NAME, names);
        return String.format(ServiceModelGeneratorConstants.HTTP_DEFAULT_LISTENER_STMT, variableName);
    }
}
