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
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.DiagnosticHandler;
import io.ballerina.flowmodelgenerator.core.model.node.ActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Assign;
import io.ballerina.flowmodelgenerator.core.model.node.BinaryData;
import io.ballerina.flowmodelgenerator.core.model.node.Break;
import io.ballerina.flowmodelgenerator.core.model.node.Comment;
import io.ballerina.flowmodelgenerator.core.model.node.Commit;
import io.ballerina.flowmodelgenerator.core.model.node.ConfigVariable;
import io.ballerina.flowmodelgenerator.core.model.node.Continue;
import io.ballerina.flowmodelgenerator.core.model.node.DataMapper;
import io.ballerina.flowmodelgenerator.core.model.node.DefaultExpression;
import io.ballerina.flowmodelgenerator.core.model.node.ErrorHandler;
import io.ballerina.flowmodelgenerator.core.model.node.EventStart;
import io.ballerina.flowmodelgenerator.core.model.node.Fail;
import io.ballerina.flowmodelgenerator.core.model.node.Foreach;
import io.ballerina.flowmodelgenerator.core.model.node.FunctionCall;
import io.ballerina.flowmodelgenerator.core.model.node.If;
import io.ballerina.flowmodelgenerator.core.model.node.JsonPayload;
import io.ballerina.flowmodelgenerator.core.model.node.Lock;
import io.ballerina.flowmodelgenerator.core.model.node.Match;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnection;
import io.ballerina.flowmodelgenerator.core.model.node.Panic;
import io.ballerina.flowmodelgenerator.core.model.node.ResourceActionCall;
import io.ballerina.flowmodelgenerator.core.model.node.Retry;
import io.ballerina.flowmodelgenerator.core.model.node.Return;
import io.ballerina.flowmodelgenerator.core.model.node.Rollback;
import io.ballerina.flowmodelgenerator.core.model.node.Start;
import io.ballerina.flowmodelgenerator.core.model.node.Stop;
import io.ballerina.flowmodelgenerator.core.model.node.Transaction;
import io.ballerina.flowmodelgenerator.core.model.node.Variable;
import io.ballerina.flowmodelgenerator.core.model.node.While;
import io.ballerina.flowmodelgenerator.core.model.node.XmlPayload;
import io.ballerina.projects.Document;
import io.ballerina.projects.ModuleDescriptor;
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
 * @since 1.4.0
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
    protected FlowNode cachedFlowNode;
    protected ModuleDescriptor moduleDescriptor;

    private static final Map<NodeKind, Supplier<? extends NodeBuilder>> CONSTRUCTOR_MAP = new HashMap<>() {{
        put(NodeKind.IF, If::new);
        put(NodeKind.RETURN, Return::new);
        put(NodeKind.EXPRESSION, DefaultExpression::new);
        put(NodeKind.ERROR_HANDLER, ErrorHandler::new);
        put(NodeKind.WHILE, While::new);
        put(NodeKind.CONTINUE, Continue::new);
        put(NodeKind.BREAK, Break::new);
        put(NodeKind.PANIC, Panic::new);
        put(NodeKind.EVENT_START, EventStart::new);
        put(NodeKind.REMOTE_ACTION_CALL, ActionCall::new);
        put(NodeKind.RESOURCE_ACTION_CALL, ResourceActionCall::new);
        put(NodeKind.NEW_CONNECTION, NewConnection::new);
        put(NodeKind.START, Start::new);
        put(NodeKind.TRANSACTION, Transaction::new);
        put(NodeKind.RETRY, Retry::new);
        put(NodeKind.LOCK, Lock::new);
        put(NodeKind.FAIL, Fail::new);
        put(NodeKind.COMMIT, Commit::new);
        put(NodeKind.ROLLBACK, Rollback::new);
        put(NodeKind.XML_PAYLOAD, XmlPayload::new);
        put(NodeKind.JSON_PAYLOAD, JsonPayload::new);
        put(NodeKind.BINARY_DATA, BinaryData::new);
        put(NodeKind.STOP, Stop::new);
        put(NodeKind.FUNCTION_CALL, FunctionCall::new);
        put(NodeKind.FOREACH, Foreach::new);
        put(NodeKind.DATA_MAPPER, DataMapper::new);
        put(NodeKind.VARIABLE, Variable::new);
        put(NodeKind.ASSIGN, Assign::new);
        put(NodeKind.COMMENT, Comment::new);
        put(NodeKind.MATCH, Match::new);
        put(NodeKind.CONFIG_VARIABLE, ConfigVariable::new);
    }};

    public static NodeBuilder getNodeFromKind(NodeKind kind) {
        return CONSTRUCTOR_MAP.getOrDefault(kind, DefaultExpression::new).get();
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

    public NodeBuilder defaultModuleName(ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
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

        if (!CommonUtils.isDefaultPackage(orgName, packageName, moduleDescriptor)) {
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
            this.formBuilder = new FormBuilder<>(semanticModel, diagnosticHandler, moduleDescriptor, this);
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

        // Check if there is a pre-built node
        if (cachedFlowNode != null) {
            return cachedFlowNode;
        }

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
