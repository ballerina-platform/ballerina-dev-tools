package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.flowmodelgenerator.core.central.Function;
import io.ballerina.flowmodelgenerator.core.central.FunctionResponse;
import io.ballerina.flowmodelgenerator.core.central.LocalIndexCentral;
import io.ballerina.flowmodelgenerator.core.central.RemoteCentral;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;
import org.ballerinalang.langserver.common.utils.DefaultValueGenerationUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FunctionCall extends NodeBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(FlowNode.Kind.FUNCTION_CALL);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();

        if (codedata.org() == null || codedata.module() == null || codedata.version() == null) {
            WorkspaceManager workspaceManager = context.workspaceManager();
            try {
                workspaceManager.loadProject(context.filePath());
                SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
                Optional<Symbol> outSymbol = semanticModel.moduleSymbols().stream()
                        .filter(symbol -> symbol.kind() == SymbolKind.FUNCTION && symbol.nameEquals(codedata.symbol()))
                        .findFirst();
                if (outSymbol.isEmpty()) {
                    throw new RuntimeException("Function not found: " + codedata.symbol());
                }

                FunctionSymbol functionSymbol = (FunctionSymbol) outSymbol.get();
                FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();

                metadata().label(codedata.symbol());
                codedata()
                        .node(FlowNode.Kind.FUNCTION_CALL)
                        .symbol(codedata.symbol());

                Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
                if (params.isPresent()) {
                    for (ParameterSymbol param : params.get()) {
                        Optional<String> name = param.getName();
                        if (name.isEmpty()) {
                            continue;
                        }
                        properties().custom(name.get(), name.get(), "", Property.ValueType.EXPRESSION,
                                param.typeDescriptor().signature(),
                                DefaultValueGenerationUtil.getDefaultValueForType(param.typeDescriptor()).orElse(""),
                                param.paramKind() != ParameterKind.REQUIRED);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error while loading the project: " + e.getMessage(), e);
            }
            properties().dataVariable(null);
            return;
        }

        FunctionResponse functionResponse = RemoteCentral.getInstance()
                .function(codedata.org(), codedata.module(), codedata.version(), codedata.symbol());
        Function function = functionResponse.data().apiDocs().docsData().modules().get(0).functions();

        metadata()
                .label(function.name())
                .description(function.description());
        codedata()
                .node(FlowNode.Kind.FUNCTION_CALL)
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .symbol(codedata.symbol());

        for (Function.Parameter parameter : function.parameters()) {
            String typeName = parameter.type().name();
            String defaultValue = parameter.defaultValue();
            String defaultString = defaultValue != null ? escapeDefaultValue(defaultValue) :
                    CommonUtils.getDefaultValueForType(typeName);
            boolean optional = defaultValue != null && !defaultValue.isEmpty();
            properties().custom(parameter.name(), parameter.name(), parameter.description(),
                    Property.ValueType.EXPRESSION, typeName, defaultString, optional);
        }

        properties().dataVariable(null);
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        if (sourceBuilder.flowNode.hasFlag(FlowNode.NODE_FLAG_CHECKED)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        FlowNode nodeTemplate = LocalIndexCentral.getInstance().getNodeTemplate(sourceBuilder.flowNode.codedata());

        String module = nodeTemplate.codedata().module();
        String methodCallPrefix = (module != null) ? module.substring(module.lastIndexOf('.') + 1) + ":" : "";
        String methodCall = methodCallPrefix + nodeTemplate.metadata().label();

        return sourceBuilder.token()
                .name(methodCall)
                .stepOut()
                .functionParameters(nodeTemplate, Set.of("variable", "type"))
                .textEdit(false)
                .acceptImport()
                .build();
    }

    private String escapeDefaultValue(String value) {
        return value.isEmpty() ? "\"\"" : value;
    }
}
