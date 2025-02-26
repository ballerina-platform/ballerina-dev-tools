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

package io.ballerina.flowmodelgenerator.core.model;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.node.AgentBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.AgentCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.AssignBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.BreakBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.CommentBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.CommitBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ConfigVariableBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ContinueBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapperDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandlerBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.EventStartBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ExpressionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FailBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ForeachBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ForkBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionCall;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionDefinitionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.IfBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayloadBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.LockBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.MatchBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.MethodCall;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.PanicBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ParallelFlowBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RemoteActionCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ResourceActionCallBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RetryBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.ReturnBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.RollbackBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.StartBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.StopBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.TransactionBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.VariableBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.WaitBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.WhileBuilder;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayloadBuilder;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.projects.Document;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Represents a builder for the flow node.
 *
 * @since 2.0.0
 */
public abstract class NodeBuilder implements DiagnosticHandler.DiagnosticCapable {

    protected List<Branch> branches;
    protected Metadata.Builder<NodeBuilder> metadataBuilder;
    protected Codedata.Builder<NodeBuilder> codedataBuilder;
    protected FormBuilder<NodeBuilder> formBuilder;
    protected Diagnostics.Builder<NodeBuilder> diagnosticBuilder;
    protected DiagnosticHandler diagnosticHandler;
    protected int flags;
    protected boolean returning;
    protected SemanticModel semanticModel;
    protected ModuleInfo moduleInfo;

    private static final Map<NodeKind, Supplier<? extends NodeBuilder>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(NodeKind.IF, IfBuilder::new);
        put(NodeKind.RETURN, ReturnBuilder::new);
        put(NodeKind.EXPRESSION, ExpressionBuilder::new);
        put(NodeKind.ERROR_HANDLER, ErrorHandlerBuilder::new);
        put(NodeKind.WHILE, WhileBuilder::new);
        put(NodeKind.CONTINUE, ContinueBuilder::new);
        put(NodeKind.BREAK, BreakBuilder::new);
        put(NodeKind.PANIC, PanicBuilder::new);
        put(NodeKind.EVENT_START, EventStartBuilder::new);
        put(NodeKind.REMOTE_ACTION_CALL, RemoteActionCallBuilder::new);
        put(NodeKind.RESOURCE_ACTION_CALL, ResourceActionCallBuilder::new);
        put(NodeKind.NEW_CONNECTION, NewConnectionBuilder::new);
        put(NodeKind.START, StartBuilder::new);
        put(NodeKind.TRANSACTION, TransactionBuilder::new);
        put(NodeKind.RETRY, RetryBuilder::new);
        put(NodeKind.LOCK, LockBuilder::new);
        put(NodeKind.FAIL, FailBuilder::new);
        put(NodeKind.COMMIT, CommitBuilder::new);
        put(NodeKind.ROLLBACK, RollbackBuilder::new);
        put(NodeKind.XML_PAYLOAD, XmlPayloadBuilder::new);
        put(NodeKind.JSON_PAYLOAD, JsonPayloadBuilder::new);
        put(NodeKind.BINARY_DATA, BinaryBuilder::new);
        put(NodeKind.STOP, StopBuilder::new);
        put(NodeKind.FUNCTION_CALL, FunctionCall::new);
        put(NodeKind.METHOD_CALL, MethodCall::new);
        put(NodeKind.FOREACH, ForeachBuilder::new);
        put(NodeKind.DATA_MAPPER, DataMapperBuilder::new);
        put(NodeKind.DATA_MAPPER_DEFINITION, DataMapperDefinitionBuilder::new);
        put(NodeKind.FUNCTION_DEFINITION, FunctionDefinitionBuilder::new);
        put(NodeKind.VARIABLE, VariableBuilder::new);
        put(NodeKind.ASSIGN, AssignBuilder::new);
        put(NodeKind.COMMENT, CommentBuilder::new);
        put(NodeKind.MATCH, MatchBuilder::new);
        put(NodeKind.CONFIG_VARIABLE, ConfigVariableBuilder::new);
        put(NodeKind.DATA_MAPPER_CALL, DataMapperCallBuilder::new);
        put(NodeKind.FORK, ForkBuilder::new);
        put(NodeKind.PARALLEL_FLOW, ParallelFlowBuilder::new);
        put(NodeKind.WAIT, WaitBuilder::new);
        put(NodeKind.AGENT, AgentBuilder::new);
        put(NodeKind.AGENT_CALL, AgentCallBuilder::new);
    }};

    public static NodeBuilder getNodeFromKind(NodeKind kind) {
        return CONSTRUCTOR_MAP.getOrDefault(kind, ExpressionBuilder::new).get();
    }

    public NodeBuilder setConstData() {
        this.setConcreteConstData();
        return this;
    }

    public abstract void setConcreteConstData();

    public NodeBuilder setTemplateData(TemplateContext context) {
        setConcreteTemplateData(context);
        return this;
    }

    public abstract void setConcreteTemplateData(TemplateContext context);

    public abstract Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder);

    public NodeBuilder() {
        this.branches = new ArrayList<>();
        this.flags = 0;
    }

    public NodeBuilder semanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        return this;
    }

    public NodeBuilder diagnosticHandler(DiagnosticHandler diagnosticHandler) {
        this.diagnosticHandler = diagnosticHandler;
        return this;
    }

    public NodeBuilder defaultModuleName(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
        return this;
    }

    public NodeBuilder returning() {
        this.returning = true;
        return this;
    }

    public NodeBuilder branch(Branch branch) {
        this.branches.add(branch);
        return this;
    }

    public NodeBuilder flag(int flag) {
        this.flags |= flag;
        return this;
    }

    public NodeBuilder symbolInfo(Symbol symbol) {
        Optional<ModuleSymbol> module = symbol.getModule();
        if (module.isEmpty()) {
            codedata()
                    .module(".")
                    .version("0.0.0");
            return this;
        }

        ModuleID moduleId = module.get().id();
        String orgName = moduleId.orgName();
        String packageName = moduleId.packageName();
        String versionName = moduleId.version();

        if (!CommonUtils.isDefaultPackage(orgName, packageName, moduleInfo)) {
            metadata().icon(CommonUtils.generateIcon(orgName, packageName, versionName));
        }
        codedata()
                .org(orgName)
                .module(packageName)
                .version(versionName);
        return this;
    }

    public Metadata.Builder<NodeBuilder> metadata() {
        if (this.metadataBuilder == null) {
            this.metadataBuilder = new Metadata.Builder<>(this);
        }
        return this.metadataBuilder;
    }

    public Codedata.Builder<NodeBuilder> codedata() {
        if (this.codedataBuilder == null) {
            this.codedataBuilder = new Codedata.Builder<>(this);
        }
        return this.codedataBuilder;
    }

    public FormBuilder<NodeBuilder> properties() {
        if (this.formBuilder == null) {
            this.formBuilder = new FormBuilder<>(semanticModel, diagnosticHandler, moduleInfo, this);
        }
        return this.formBuilder;
    }

    public Diagnostics.Builder<NodeBuilder> diagnostics() {
        if (this.diagnosticBuilder == null) {
            this.diagnosticBuilder = new Diagnostics.Builder<>(this);
        }
        return this.diagnosticBuilder;
    }

    public FlowNode build() {
        this.setConstData();

        Codedata codedata = codedataBuilder == null ? null : codedataBuilder.build();
        return new FlowNode(
                String.valueOf(Objects.hash(codedata != null ? codedata.lineRange() : null)),
                metadataBuilder == null ? null : metadataBuilder.build(),
                codedata,
                returning,
                branches.isEmpty() ? null : branches,
                formBuilder == null ? null : formBuilder.build(),
                diagnosticBuilder == null ? null : diagnosticBuilder.build(),
                flags
        );
    }

    public AvailableNode buildAvailableNode() {
        this.setConcreteConstData();
        return new AvailableNode(metadataBuilder == null ? null : metadataBuilder.build(),
                codedataBuilder == null ? null : codedataBuilder.build(), true);
    }

    public record TemplateContext(WorkspaceManager workspaceManager, Path filePath, LinePosition position,
                                  Codedata codedata) {

        public Set<String> getAllVisibleSymbolNames() {
            try {
                workspaceManager.loadProject(filePath);
                SemanticModel semanticModel =
                        workspaceManager.semanticModel(filePath).orElseThrow();
                Document document = workspaceManager.document(filePath).orElseThrow();
                return semanticModel.visibleSymbols(document, position).parallelStream()
                        .filter(s -> s.getName().isPresent())
                        .map(s -> s.getName().get())
                        .collect(Collectors.toSet());
            } catch (Throwable e) {
                return new HashSet<>();
            }
        }
    }
}
