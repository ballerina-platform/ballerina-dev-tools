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
import io.ballerina.compiler.api.symbols.AnnotationAttachmentSymbol;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.values.ConstantValue;
import io.ballerina.compiler.syntax.tree.AssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.BlockStatementNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ClientResourceAccessActionNode;
import io.ballerina.compiler.syntax.tree.CompoundAssignmentStatementNode;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionStatementNode;
import io.ballerina.compiler.syntax.tree.FailStatementNode;
import io.ballerina.compiler.syntax.tree.ForEachStatementNode;
import io.ballerina.compiler.syntax.tree.ForkStatementNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IfElseStatementNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ListenerDeclarationNode;
import io.ballerina.compiler.syntax.tree.LockStatementNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MatchStatementNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.PanicStatementNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.RetryStatementNode;
import io.ballerina.compiler.syntax.tree.ReturnStatementNode;
import io.ballerina.compiler.syntax.tree.RollbackStatementNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.StartActionNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TemplateExpressionNode;
import io.ballerina.compiler.syntax.tree.TransactionStatementNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.WhileStatementNode;
import io.ballerina.designmodelgenerator.core.model.Connection;
import io.ballerina.designmodelgenerator.core.model.Listener;
import io.ballerina.designmodelgenerator.core.model.Location;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Code analyzer to analyze ST and update the intermediate model.
 *
 * @since 2.0.0
 */
public class CodeAnalyzer extends NodeVisitor {

    private final SemanticModel semanticModel;
    private final IntermediateModel intermediateModel;
    private IntermediateModel.FunctionModel currentFunctionModel;
    private IntermediateModel.ServiceModel currentServiceModel;
    private final Path rootPath;
    private int serviceCount;

    public CodeAnalyzer(SemanticModel semanticModel, IntermediateModel intermediateModel, Path rootPath) {
        this.semanticModel = semanticModel;
        this.intermediateModel = intermediateModel;
        this.rootPath = rootPath;
        this.serviceCount = 0;
    }

    @Override
    public void visit(ModulePartNode modulePartNode) {
        modulePartNode.members().forEach(member -> member.accept(this));
    }

    @Override
    public void visit(ServiceDeclarationNode serviceDeclarationNode) {
        IntermediateModel.ServiceModel serviceModel = new IntermediateModel.ServiceModel("listener");
        this.currentServiceModel = serviceModel;
        serviceDeclarationNode.members().forEach(member -> member.accept(this));
        serviceModel.location = getLocation(serviceDeclarationNode.lineRange());
        intermediateModel.serviceModelMap.put(getServiceName(serviceDeclarationNode.lineRange().fileName()),
                serviceModel);
        this.currentServiceModel = null;
    }

    @Override
    public void visit(FunctionDefinitionNode functionDefinitionNode) {
        String functionName = functionDefinitionNode.functionName().text();
        this.currentFunctionModel = new IntermediateModel.FunctionModel(functionDefinitionNode.functionName().text());
        if (this.currentServiceModel != null) {
            Optional<Symbol> symbol = this.semanticModel.symbol(functionDefinitionNode);
            if (symbol.isPresent()) {
                MethodSymbol methodSymbol = (MethodSymbol) symbol.get();
                if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                    this.currentFunctionModel.path = ((ResourceMethodSymbol) methodSymbol).resourcePath().signature();
                    this.currentServiceModel.resourceFunctions.add(this.currentFunctionModel);
                } else if (methodSymbol.qualifiers().contains(Qualifier.REMOTE)) {
                    this.currentServiceModel.remoteFunctions.add(this.currentFunctionModel);
                } else {
                    this.currentServiceModel.otherFunctions.add(this.currentFunctionModel);
                }
            }
        } else {
            intermediateModel.functionModelMap.put(functionDefinitionNode.functionName().text(),
                    this.currentFunctionModel);
        }
        this.currentFunctionModel.location = getLocation(functionDefinitionNode.lineRange());
        if (functionName.equals(DesignModelGenerator.MAIN_FUNCTION_NAME)) {
            Optional<Symbol> symbol = this.semanticModel.symbol(functionDefinitionNode);
            if (symbol.isPresent()) {
                this.currentFunctionModel.displayName =
                        getDisplayName(((FunctionSymbol) symbol.get()).annotAttachments());
            }
        }
        functionDefinitionNode.functionBody().accept(this);
        this.currentFunctionModel = null;
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        for (StatementNode statement : functionBodyBlockNode.statements()) {
            statement.accept(this);
        }
        super.visit(functionBodyBlockNode);
    }

    @Override
    public void visit(DoStatementNode doStatementNode) {
        BlockStatementNode blockStatementNode = doStatementNode.blockStatement();
        blockStatementNode.statements().forEach(statement -> statement.accept(this));
        doStatementNode.onFailClause()
                .ifPresent(onFailClauseNode -> onFailClauseNode.blockStatement()
                        .statements()
                        .forEach(statement -> statement.accept(this)));
    }

    @Override
    public void visit(FunctionCallExpressionNode functionCallExpressionNode) {
        if (!(functionCallExpressionNode.functionName() instanceof QualifiedNameReferenceNode)) {
            if (this.currentFunctionModel != null) {
                this.currentFunctionModel.dependentFuncs.add(functionCallExpressionNode.functionName().toSourceCode());
            }
            functionCallExpressionNode.arguments().forEach(arg -> arg.accept(this));
        }
    }

    @Override
    public void visit(RemoteMethodCallActionNode remoteMethodCallActionNode) {
        if (this.currentFunctionModel != null) {
            this.currentFunctionModel.connections.add(remoteMethodCallActionNode.expression().toSourceCode());
        }
        remoteMethodCallActionNode.arguments().forEach(arg -> arg.accept(this));
    }

    @Override
    public void visit(ClientResourceAccessActionNode clientResourceAccessActionNode) {
        if (this.currentFunctionModel != null) {
            this.currentFunctionModel.connections.add(clientResourceAccessActionNode.expression().toSourceCode());
        }
        clientResourceAccessActionNode.arguments().ifPresent(parenthesizedArgList -> parenthesizedArgList.arguments()
                .forEach(expr -> expr.accept(this)));
    }

    @Override
    public void visit(ReturnStatementNode returnStatementNode) {
        returnStatementNode.expression().ifPresent(expr -> expr.accept(this));
    }

    @Override
    public void visit(IfElseStatementNode ifElseStatementNode) {
        ifElseStatementNode.condition().accept(this);
        ifElseStatementNode.ifBody().statements().forEach(statement -> statement.accept(this));
        ifElseStatementNode.elseBody().ifPresent(elseBody -> elseBody.accept(this));
    }

    @Override
    public void visit(ImplicitNewExpressionNode implicitNewExpressionNode) {
        implicitNewExpressionNode.parenthesizedArgList()
                .ifPresent(parenthesizedArgList -> parenthesizedArgList.arguments()
                        .forEach(expr -> expr.accept(this)));
    }

    @Override
    public void visit(ExplicitNewExpressionNode explicitNewExpressionNode) {
        explicitNewExpressionNode.parenthesizedArgList().arguments().forEach(expr -> expr.accept(this));
    }

    @Override
    public void visit(TemplateExpressionNode templateExpressionNode) {
        templateExpressionNode.content().forEach(expr -> expr.accept(this));
    }

    @Override
    public void visit(VariableDeclarationNode variableDeclarationNode) {
        variableDeclarationNode.initializer().ifPresent(expr -> expr.accept(this));
    }

    @Override
    public void visit(ListenerDeclarationNode listenerDeclarationNode) {
        this.intermediateModel.listeners.add(
                new Listener(listenerDeclarationNode.variableName().text(),
                        getLocation(listenerDeclarationNode.lineRange())));
    }

    @Override
    public void visit(ModuleVariableDeclarationNode moduleVariableDeclarationNode) {
        Optional<Symbol> symbol = semanticModel.symbol(moduleVariableDeclarationNode);
        if (symbol.isPresent()) {
            VariableSymbol variableSymbol = (VariableSymbol) symbol.get();
            TypeSymbol varType = CommonUtils.getRawType(variableSymbol.typeDescriptor());
            if (variableSymbol.getName().isEmpty() || !(varType instanceof ObjectTypeSymbol objType)) {
                return;
            }
            if (objType.qualifiers().contains(Qualifier.CLIENT)) {
                LineRange lineRange = variableSymbol.getLocation().get().lineRange();
                this.intermediateModel.connections.add(
                        new Connection(variableSymbol.getName().get(), getLocation(lineRange)));
            }
        }
        moduleVariableDeclarationNode.initializer().ifPresent(expr -> expr.accept(this));
    }

    @Override
    public void visit(AssignmentStatementNode assignmentStatementNode) {
        assignmentStatementNode.expression().accept(this);
    }

    @Override
    public void visit(CompoundAssignmentStatementNode compoundAssignmentStatementNode) {
        compoundAssignmentStatementNode.rhsExpression().accept(this);
        compoundAssignmentStatementNode.lhsExpression().accept(this);
    }

    @Override
    public void visit(BlockStatementNode blockStatementNode) {
        blockStatementNode.statements().forEach(statement -> statement.accept(this));
    }

    @Override
    public void visit(FailStatementNode failStatementNode) {
        failStatementNode.expression().accept(this);
    }

    @Override
    public void visit(ExpressionStatementNode expressionStatementNode) {
        expressionStatementNode.expression().accept(this);
    }

    @Override
    public void visit(WhileStatementNode whileStatementNode) {
        whileStatementNode.condition().accept(this);
        whileStatementNode.whileBody().statements().forEach(statement -> statement.accept(this));
        whileStatementNode.onFailClause().ifPresent(onFailClauseNode -> onFailClauseNode.blockStatement()
                .statements().forEach(statement -> statement.accept(this)));
    }

    @Override
    public void visit(PanicStatementNode panicStatementNode) {
        panicStatementNode.expression().accept(this);
    }

    @Override
    public void visit(CheckExpressionNode checkExpressionNode) {
        checkExpressionNode.expression().accept(this);
    }

    @Override
    public void visit(StartActionNode startActionNode) {
        startActionNode.expression().accept(this);
    }

    @Override
    public void visit(LockStatementNode lockStatementNode) {
        lockStatementNode.blockStatement().statements().forEach(statement -> statement.accept(this));
    }

    @Override
    public void visit(ForkStatementNode forkStatementNode) {
        forkStatementNode.namedWorkerDeclarations().forEach(namedWorkerDeclaration -> {
            namedWorkerDeclaration.accept(this);
        });
    }

    @Override
    public void visit(NamedWorkerDeclarationNode namedWorkerDeclarationNode) {
        namedWorkerDeclarationNode.workerBody().statements().forEach(statement -> statement.accept(this));
        namedWorkerDeclarationNode.onFailClause().ifPresent(onFailClauseNode -> onFailClauseNode.blockStatement()
                .statements().forEach(statement -> statement.accept(this)));
    }

    @Override
    public void visit(TransactionStatementNode transactionStatementNode) {
        transactionStatementNode.blockStatement().statements().forEach(statement -> statement.accept(this));
        transactionStatementNode.onFailClause().ifPresent(onFailClauseNode -> onFailClauseNode.blockStatement()
                .statements().forEach(statement -> statement.accept(this)));
    }

    @Override
    public void visit(ForEachStatementNode forEachStatementNode) {
        forEachStatementNode.blockStatement().statements().forEach(statement -> statement.accept(this));
        forEachStatementNode.actionOrExpressionNode().accept(this);
    }

    @Override
    public void visit(RollbackStatementNode rollbackStatementNode) {
        rollbackStatementNode.expression().ifPresent(expr -> expr.accept(this));
    }

    @Override
    public void visit(RetryStatementNode retryStatementNode) {
        retryStatementNode.retryBody().accept(this);
        retryStatementNode.onFailClause().ifPresent(onFailClauseNode -> onFailClauseNode.blockStatement()
                .statements().forEach(statement -> statement.accept(this)));
    }

    @Override
    public void visit(MatchStatementNode matchStatementNode) {
        matchStatementNode.condition().accept(this);
        matchStatementNode.matchClauses().forEach(matchClause -> {
            matchClause.blockStatement().statements().forEach(statement -> statement.accept(this));
        });
        matchStatementNode.onFailClause().ifPresent(onFailClauseNode -> onFailClauseNode.blockStatement()
                .statements().forEach(statement -> statement.accept(this)));
    }

    @Override
    public void visit(MappingConstructorExpressionNode mappingConstructorExpressionNode) {
        mappingConstructorExpressionNode.fields().forEach(field -> field.accept(this));
    }

    @Override
    public void visit(ListConstructorExpressionNode listConstructorExpressionNode) {
        listConstructorExpressionNode.expressions().forEach(expr -> expr.accept(this));
    }

    private String getServiceName(String fileName) {
        String name = Arrays.stream(fileName.split("\\.")).toList().get(0);
        name = serviceCount > 0 ? name + "_" + serviceCount : name;
        serviceCount++;
        return name;
    }

    private String getDisplayName(List<AnnotationAttachmentSymbol> annotationAttachmentSymbols) {
        return annotationAttachmentSymbols
                .stream()
                .filter(annotationAttachmentSymbol -> {
                    AnnotationSymbol annotationSymbol = annotationAttachmentSymbol.typeDescriptor();
                    if (annotationSymbol.getName().isPresent()) {
                        return annotationSymbol.getName().get().equals("display");
                    }
                    return false;
                })
                .findAny()
                .map(annotationAttachmentSymbol -> annotationAttachmentSymbol
                        .attachmentValue()
                        .map(ConstantValue::value))
                .flatMap(v -> ((Optional<?>) v))
                .map(m -> ((Map<?, ?>) m).get("label"))
                .map(v -> ((ConstantValue) v).value().toString())
                .orElse("");
    }

    private Location getLocation(LineRange lineRange) {
        Path filePath = rootPath.resolve(lineRange.fileName());
        return new Location(filePath.toAbsolutePath().toString(), lineRange.startLine(),
                lineRange.endLine());
    }
}
