package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.central.Central;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.SourceBuilder;

import java.util.Set;

public class FunctionCall extends NodeBuilder {

    private final Central central = new CentralProxy();

    @Override
    public void setConcreteConstData() {
        codedata().node(FlowNode.Kind.FUNCTION_CALL);
    }

    @Override
    public void setConcreteTemplateData(Codedata codedata) {
        this.cachedFlowNode = central.getNodeTemplate(codedata);
    }

    @Override
    public String toSource(FlowNode flowNode) {
        SourceBuilder sourceBuilder = new SourceBuilder(flowNode)
                .newVariable();

        if (flowNode.hasFlag(FlowNode.NODE_FLAG_CHECKED)) {
            sourceBuilder.token().keyword(SyntaxKind.CHECK_KEYWORD);
        }

        FlowNode nodeTemplate = central.getNodeTemplate(flowNode.codedata());
        return sourceBuilder.token()
                .name(nodeTemplate.metadata().label())
                .stepOut()
                .functionParameters(nodeTemplate, Set.of("variable", "type"))
                .build(false);
    }
}
