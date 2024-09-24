package io.ballerina.flowmodelgenerator.core.model.node;

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
import org.eclipse.lsp4j.TextEdit;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FunctionCall extends NodeBuilder {

    @Override
    public void setConcreteConstData() {
        codedata().node(FlowNode.Kind.FUNCTION_CALL);
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
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
