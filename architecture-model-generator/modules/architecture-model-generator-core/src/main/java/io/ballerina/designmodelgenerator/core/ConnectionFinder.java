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

package io.ballerina.designmodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassFieldSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ObjectFieldNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.designmodelgenerator.core.model.Connection;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Find connections for the given symbol.
 *
 * @since 2.0.0
 */
public class ConnectionFinder {

    private final SemanticModel semanticModel;
    private final Map<String, ModulePartNode> documentMap;
    private final Path rootPath;
    private final IntermediateModel intermediateModel;


    public ConnectionFinder(SemanticModel semanticModel, Path rootPath,
                            Map<String, ModulePartNode> documentMap,
                            IntermediateModel intermediateModel) {
        this.semanticModel = semanticModel;
        this.documentMap = documentMap;
        this.rootPath = rootPath;
        this.intermediateModel = intermediateModel;
    }

    public void findConnection(Symbol symbol, List<String> referenceLocations) {
        String hashKey = String.valueOf(symbol.getLocation().get().hashCode());
        referenceLocations.add(hashKey);
        if (this.intermediateModel.connectionMap.containsKey(hashKey)) {
            Connection connection = this.intermediateModel.connectionMap.get(hashKey);
            for (String refLocation : referenceLocations) {
                intermediateModel.connectionMap.put(refLocation, connection);
            }
        }
        if (symbol instanceof ClassFieldSymbol classFieldSymbol) {
            if (classFieldSymbol.hasDefaultValue()) {
                Location location = classFieldSymbol.getLocation().get();
                ModulePartNode modulePartNode = documentMap.get(location.lineRange().fileName());
                NonTerminalNode node = modulePartNode.findNode(location.textRange());
                if (node instanceof ObjectFieldNode objectFieldNode) {
                    if (isNewConnection(objectFieldNode.expression().orElse(null))) {
                        LineRange lineRange = node.lineRange();
                        String sortText = lineRange.fileName() + lineRange.startLine().line();
                        String icon =  CommonUtils.generateIcon(
                                classFieldSymbol.typeDescriptor().getModule().get().id());
                        Connection connection = new Connection(objectFieldNode.fieldName().text(),
                                sortText, getLocation(lineRange), Connection.Scope.LOCAL, icon);
                        for (String refLocation : referenceLocations) {
                            intermediateModel.connectionMap.put(String.valueOf(refLocation), connection);
                        }
                    } else {
                        Optional<Symbol> valueSymbol = semanticModel.symbol(objectFieldNode.expression().get());
                        if (valueSymbol.isPresent()) { // TODO: handle for function calls
                            findConnection(valueSymbol.get(), referenceLocations);
                        }
                    }
                }
            } else {
                List<Location> references = this.semanticModel.references(classFieldSymbol);
                for (Location location : references) {
                    ModulePartNode modulePartNode = documentMap.get(location.lineRange().fileName());
                    NonTerminalNode node = modulePartNode.findNode(location.textRange()).parent();
                    if (node instanceof AssignmentStatementNode assignmentStatementNode) {
                        if (isNewConnection(assignmentStatementNode.expression())) {
                            LineRange lineRange = node.lineRange();
                            String sortText = lineRange.fileName() + lineRange.startLine().line();
                            String icon =  CommonUtils.generateIcon(
                                    classFieldSymbol.typeDescriptor().getModule().get().id());
                            Connection connection = new Connection(symbol.getName().get(), sortText,
                                    getLocation(lineRange), Connection.Scope.LOCAL, icon);
                            for (String refLocation : referenceLocations) {
                                intermediateModel.connectionMap.put(String.valueOf(refLocation), connection);
                            }
                        } else {
                            Optional<Symbol> valueSymbol = semanticModel.symbol(assignmentStatementNode.expression());
                            if (valueSymbol.isPresent()) { // TODO: handle for function calls
                                findConnection(valueSymbol.get(), referenceLocations);
                            }
                        }
                    }
                }
            }
        } else if (symbol instanceof VariableSymbol variableSymbol) {
            if (this.intermediateModel.connectionMap.containsKey(hashKey)) {
                Connection connection = this.intermediateModel.connectionMap.get(hashKey);
                for (String refLocation : referenceLocations) {
                    intermediateModel.connectionMap.put(refLocation, connection);
                }
            } else {
                List<Location> references = this.semanticModel.references(variableSymbol);
                for (Location location : references) {
                    ModulePartNode modulePartNode = documentMap.get(location.lineRange().fileName());
                    NonTerminalNode node = modulePartNode.findNode(location.textRange()).parent();
                    if (node instanceof VariableDeclarationNode variableDeclarationNode) {
                        if (isNewConnection(variableDeclarationNode.initializer().orElse(null))) {
                            LineRange lineRange = node.lineRange();
                            String sortText = lineRange.fileName() + lineRange.startLine().line();
                            String icon =  CommonUtils.generateIcon(
                                    variableSymbol.typeDescriptor().getModule().get().id());
                            Connection connection = new Connection(symbol.getName().get(), sortText,
                                    getLocation(lineRange), Connection.Scope.LOCAL, icon, true);
                            for (String refLocation : referenceLocations) {
                                intermediateModel.connectionMap.put(String.valueOf(refLocation), connection);
                            }
                        } else {
                            if (variableDeclarationNode.initializer().isPresent()) {
                                Optional<Symbol> valueSymbol = semanticModel.symbol(
                                        variableDeclarationNode.initializer().get());
                                if (valueSymbol.isPresent()) { // TODO: handle for function calls
                                    findConnection(valueSymbol.get(), referenceLocations);
                                }
                            }
                        }
                    } else if (node instanceof AssignmentStatementNode assignmentStatementNode) {
                        if (isNewConnection(assignmentStatementNode.expression())) {
                            LineRange lineRange = node.lineRange();
                            String sortText = lineRange.fileName() + lineRange.startLine().line();
                            Connection connection = new Connection(symbol.getName().get(), sortText,
                                    getLocation(lineRange), Connection.Scope.LOCAL, "");
                            for (String refLocation : referenceLocations) {
                                intermediateModel.connectionMap.put(String.valueOf(refLocation), connection);
                            }
                        } else {
                            Optional<Symbol> valueSymbol = semanticModel.symbol(assignmentStatementNode.expression());
                            if (valueSymbol.isPresent()) { // TODO: handle for function calls
                                findConnection(valueSymbol.get(), referenceLocations);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isNewConnection(ExpressionNode expressionNode) {
        if (expressionNode == null) {
            return false;
        } else if (expressionNode instanceof ExplicitNewExpressionNode
                || expressionNode instanceof ImplicitNewExpressionNode) {
            return true;
        } else if (expressionNode instanceof CheckExpressionNode checkExpressionNode) {
            return isNewConnection(checkExpressionNode.expression());
        }
        return false;
    }

    public io.ballerina.designmodelgenerator.core.model.Location getLocation(LineRange lineRange) {
        Path filePath = rootPath.resolve(lineRange.fileName());
        return new io.ballerina.designmodelgenerator.core.model.Location(
                filePath.toAbsolutePath().toString(), lineRange.startLine(),
                lineRange.endLine());
    }
}
