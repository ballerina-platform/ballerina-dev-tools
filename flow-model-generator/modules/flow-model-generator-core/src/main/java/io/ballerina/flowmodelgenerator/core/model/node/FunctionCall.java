package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.flowmodelgenerator.core.central.Central;
import io.ballerina.flowmodelgenerator.core.central.CentralProxy;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;

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
        return null;
    }
}
