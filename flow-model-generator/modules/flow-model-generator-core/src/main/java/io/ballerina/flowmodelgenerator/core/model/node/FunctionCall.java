package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
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
        this.cachedFlowNode = CentralProxy.getInstance().getNodeTemplate(context.codedata());
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder.newVariable();

        if (sourceBuilder.flowNode.hasFlag(FlowNode.NODE_FLAG_CHECKED)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        FlowNode nodeTemplate = CentralProxy.getInstance().getNodeTemplate(sourceBuilder.flowNode.codedata());
        return sourceBuilder.token()
                .name(nodeTemplate.metadata().label())
                .stepOut()
                .functionParameters(nodeTemplate, Set.of("variable", "type"))
                .textEdit(false)
                .acceptImport()
                .build();
    }
}
