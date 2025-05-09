package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.flowmodelgenerator.core.Constants;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.FormBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeBuilder;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.Property;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.FunctionData;
import io.ballerina.modelgenerator.commons.FunctionDataBuilder;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.modelgenerator.commons.ParameterData;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;

public class NPFunctionCall extends FunctionCall {

    public static final String LABEL = "Call Natural Function";
    public static final String DESCRIPTION = "Call a natural programming function";

    @Override
    protected NodeKind getFunctionNodeKind() {
        return NodeKind.NP_FUNCTION_CALL;
    }

    @Override
    protected FunctionData.Kind getFunctionResultKind() {
        return super.getFunctionResultKind();
    }

    @Override
    protected void setParameterProperties(FunctionData function) {
        boolean hasOnlyRestParams = function.parameters().size() == 1;
        for (ParameterData paramResult : function.parameters().values()) {
            if (paramResult.kind().equals(ParameterData.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(ParameterData.Kind.INCLUDED_RECORD)) {
                continue;
            }

            if (isPromptParam(paramResult)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .imports(paramResult.importStatements())
                    .editable()
                    .defaultable(paramResult.optional());

            switch (paramResult.kind()) {
                case INCLUDED_RECORD_REST -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    unescapedParamName = "additionalValues";
                    customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
                }
                case REST_PARAMETER -> {
                    if (hasOnlyRestParams) {
                        customPropBuilder.defaultable(false);
                    }
                    customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
                }
                default -> customPropBuilder.type(Property.ValueType.EXPRESSION);
            }

            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();

        FunctionDataBuilder functionDataBuilder = new FunctionDataBuilder()
                .name(codedata.symbol())
                .moduleInfo(new ModuleInfo(codedata.org(), codedata.module(), codedata.module(), codedata.version()))
                .functionResultKind(getFunctionResultKind())
                .userModuleInfo(moduleInfo);

        // Set the semantic model if the function is local
        boolean isLocalFunction = isLocalFunction(context.workspaceManager(), context.filePath(), codedata);
        if (isLocalFunction) {
            WorkspaceManager workspaceManager = context.workspaceManager();
            PackageUtil.loadProject(context.workspaceManager(), context.filePath());
            context.workspaceManager().module(context.filePath())
                    .map(module -> ModuleInfo.from(module.descriptor()))
                    .ifPresent(functionDataBuilder::userModuleInfo);
            SemanticModel semanticModel = workspaceManager.semanticModel(context.filePath()).orElseThrow();
            functionDataBuilder.semanticModel(semanticModel);
        }
        FunctionData functionData = functionDataBuilder.build();

        metadata()
                .label(functionData.name())
                .description(functionData.description());
        codedata()
                .id(functionData.functionId())
                .node(getFunctionNodeKind())
                .org(codedata.org())
                .module(codedata.module())
                .object(codedata.object())
                .version(codedata.version())
                .symbol(codedata.symbol());
        setParameterProperties(functionData);

        String returnTypeName = functionData.returnType();
        if (CommonUtils.hasReturn(functionData.returnType())) {
            properties()
                    .type(returnTypeName, functionData.inferredReturnType(), functionData.importStatements())
                    .data(returnTypeName, context.getAllVisibleSymbolNames(), Property.VARIABLE_NAME);
        }

        if (functionData.returnError()) {
            properties().checkError(true);
        }
    }

    public static boolean isPromptParam(ParameterData parameterData) {
        return parameterData.name().equals(Constants.NaturalFunctions.PROMPT)
                && parameterData.type().equals(Constants.NaturalFunctions.MODULE_PREFIXED_PROMPT_TYPE);
    }
}
