package io.ballerina.servicemodelgenerator.extension.util;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.ServiceDatabaseManager;
import io.ballerina.modelgenerator.commons.ServiceDeclaration;
import io.ballerina.modelgenerator.commons.ServiceTypeFunction;
import io.ballerina.servicemodelgenerator.extension.ServiceModelGeneratorConstants;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.DisplayAnnotation;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static io.ballerina.servicemodelgenerator.extension.util.Utils.getFunctionModel;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getPath;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.getResourceFunctionModel;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.isPresent;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateFunction;
import static io.ballerina.servicemodelgenerator.extension.util.Utils.updateFunctionInfo;

public class ServiceModelUtils {

    /**
     * Update the service model of services other than http.
     *
     * @param serviceModel service model initially populated using the database
     * @param serviceNode service declaration node
     * @param semanticModel semantic model of the source
     */
    public static void updateGenericServiceModel(Service serviceModel, ServiceDeclarationNode serviceNode,
                                                 SemanticModel semanticModel) {
        // handle base path and string literal
        String attachPoint = getPath(serviceNode.absoluteResourcePath());
        if (!attachPoint.isEmpty()) {
            boolean isStringLiteral = attachPoint.startsWith("\"") && attachPoint.endsWith("\"");
            if (isStringLiteral) {
                Value stringLiteralProperty = serviceModel.getStringLiteralProperty();
                if (Objects.nonNull(stringLiteralProperty)) {
                    stringLiteralProperty.setValue(attachPoint);
                } else {
                    serviceModel.setStringLiteral(ServiceModelUtils.getStringLiteralProperty(attachPoint));
                }
            } else {
                Value basePathProperty = serviceModel.getBasePath();
                if (Objects.nonNull(basePathProperty)) {
                    basePathProperty.setValue(attachPoint);
                } else {
                    serviceModel.setBasePath(ServiceModelUtils.getBasePathProperty(attachPoint));
                }
            }
        }

        boolean isGraphql = serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.GRAPHQL);
        List<Function> functionsInSource = serviceNode.members().stream()
                .filter(member -> member instanceof FunctionDefinitionNode functionDefinitionNode)
                .map(member -> getFunctionModel((FunctionDefinitionNode) member, semanticModel, false, isGraphql))
                .toList();

        updateServiceInfoNew(serviceModel, functionsInSource);
        serviceModel.setCodedata(new Codedata(serviceNode.lineRange()));
    }

    private static void updateServiceInfoNew(Service serviceModel, List<Function> functionsInSource) {
        Utils.populateRequiredFunctions(serviceModel);

        // mark the enabled functions as true if they present in the source
        serviceModel.getFunctions().forEach(functionModel -> {
            Optional<Function> function = functionsInSource.stream()
                    .filter(newFunction -> isPresent(functionModel, newFunction)
                            && newFunction.getKind().equals(functionModel.getKind()))
                    .findFirst();
            function.ifPresentOrElse(
                    func -> updateFunction(functionModel, func, serviceModel),
                    () -> functionModel.setEnabled(false)
            );
        });

        // functions contains in source but not enforced using the service contract type
        functionsInSource.forEach(funcInSource -> {
            if (serviceModel.getFunctions().stream().noneMatch(newFunction -> isPresent(funcInSource, newFunction))) {
                if (serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.HTTP) &&
                        funcInSource.getKind().equals(ServiceModelGeneratorConstants.KIND_RESOURCE)) {
                    getResourceFunctionModel().ifPresentOrElse(
                            resourceFunction -> {
                                updateFunctionInfo(resourceFunction, funcInSource);
                                serviceModel.addFunction(resourceFunction);
                            },
                            () -> serviceModel.addFunction(funcInSource)
                    );
                } else if (serviceModel.getModuleName().equals(ServiceModelGeneratorConstants.GRAPHQL)) {
                    GraphqlUtil.updateGraphqlFunctionMetaData(funcInSource);
                    serviceModel.addFunction(funcInSource);
                } else {
                    serviceModel.addFunction(funcInSource);
                }
            }
        });
    }


    public static Optional<Service> getEmptyServiceModel(String moduleName) {
        if (moduleName.equals("http")) {
            return getHttpService();
        }
        Optional<ServiceDeclaration> serviceDeclaration = ServiceDatabaseManager.getInstance()
                .getServiceDeclaration(moduleName);
        if (serviceDeclaration.isEmpty()) {
            return Optional.empty();
        }
        ServiceDeclaration serviceTemplate = serviceDeclaration.get();
        ServiceDeclaration.Package pkg = serviceTemplate.packageInfo();

        String protocol = getProtocol(moduleName);

        // TODO: introduce a new label field
        String label = protocol + " Service";
        String documentation = "Add the service documentation";
        String icon = CommonUtils.generateIcon(pkg.org(), pkg.name(), pkg.version());

        Map<String, Value> properties = new LinkedHashMap<>();

        Service.ServiceModelBuilder serviceBuilder = new Service.ServiceModelBuilder();
        serviceBuilder
                .setId(String.valueOf(pkg.packageId()))
                .setName(label)
                .setType(moduleName)
                .setDisplayName(label)
                .setDescription(documentation)
                .setDisplayAnnotation(new DisplayAnnotation(label, icon))
                .setModuleName(moduleName)
                .setOrgName(pkg.org())
                .setVersion(pkg.version())
                .setPackageName(pkg.name())
                .setListenerProtocol(protocol)
                .setIcon(icon)
                .setProperties(properties)
                .setFunctions(new ArrayList<>());

        Service service = serviceBuilder.build();
        properties.put("listener", getListenersProperty(protocol, serviceTemplate.listenerKind()));

        // type descriptor
        properties.put("serviceType", getTypeDescriptorProperty(serviceTemplate, pkg.packageId()));

        // base path
        if (serviceTemplate.optionalAbsoluteResourcePath() == 0) {
            properties.put("basePath", getBasePathProperty(serviceTemplate));
        }

        // string literal
        if (serviceTemplate.optionalStringLiteral() == 0) {
            properties.put("stringLiteral", getStringLiteral(serviceTemplate));
        }

        return Optional.of(service);
    }

    public static Optional<Service> getServiceModelWithFunctions(String moduleName, String serviceType) {
        Optional<Service> serviceOptional = getEmptyServiceModel(moduleName);
        if (serviceOptional.isEmpty() || serviceOptional.get().getPackageName().equals("http")) {
            return serviceOptional;
        }
        Service service = serviceOptional.get();
        int packageId = Integer.parseInt(service.getId());
        ServiceDatabaseManager.getInstance().getMatchingServiceTypeFunctions(packageId, serviceType)
                .forEach(function -> service.getFunctions().add(getFunction(function)));
        service.getServiceType().setValue(serviceType);
        return Optional.of(service);
    }

    public static Function getFunction(ServiceTypeFunction function) {

        List<Parameter> parameters = new ArrayList<>();
        for (ServiceTypeFunction.ServiceTypeFunctionParameter parameter : function.parameters()) {
            parameters.add(getParameter(parameter));
        }

        Value.ValueBuilder functionName = new Value.ValueBuilder();
        functionName
                .setMetadata(new MetaData(function.name(), function.description()))
                .setCodedata(new Codedata("FUNCTION_NAME"))
                .setValue(function.name())
                .setValueType("IDENTIFIER")
                .setValueTypeConstraint("string")
                .setPlaceholder(function.name())
                .setEnabled(function.enable() == 1)
                .setEditable(false)
                .setType(false)
                .setOptional(false)
                .setAdvanced(false);

        Value.ValueBuilder functionReturnType = new Value.ValueBuilder();
        functionReturnType
                .setMetadata(new MetaData("Return Type", "The return type of the function"))
                .setValue(function.returnType())
                .setValueType("TYPE")
                .setValueTypeConstraint("string")
                .setPlaceholder(function.returnType())
                .setEnabled(true)
                .setEditable(true)
                .setType(true)
                .setOptional(true)
                .setAdvanced(false);

        Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
        functionBuilder
                .setMetadata(new MetaData(function.name(), function.description()))
                .setKind(function.kind())
                .setEnabled(true)
                .setOptional(false)
                .setEditable(true)
                .setName(functionName.build())
                .setReturnType(new FunctionReturnType(functionReturnType.build()))
                .setParameters(parameters);

        return functionBuilder.build();
    }

    public static Parameter getParameter(ServiceTypeFunction.ServiceTypeFunctionParameter parameter) {
        Value.ValueBuilder parameterName = new Value.ValueBuilder();
        parameterName
                .setMetadata(new MetaData(parameter.name(), parameter.description()))
                .setCodedata(new Codedata("PARAMETER_NAME"))
                .setValue(parameter.name())
                .setValueType("IDENTIFIER")
                .setValueTypeConstraint("string")
                .setPlaceholder(parameter.name())
                .setEnabled(true)
                .setEditable(false)
                .setType(false)
                .setOptional(false)
                .setAdvanced(false);

        Value.ValueBuilder parameterType = new Value.ValueBuilder();
        parameterType
                .setMetadata(new MetaData("Type", "The type of the parameter"))
                .setValue(parameter.type())
                .setValueType("TYPE")
                .setValueTypeConstraint("string")
                .setPlaceholder(parameter.type())
                .setEnabled(true)
                .setEditable(true)
                .setType(true)
                .setOptional(true)
                .setAdvanced(false);

        Value.ValueBuilder parameterDefaultValue = new Value.ValueBuilder();
        parameterDefaultValue
                .setMetadata(new MetaData("Default Value", "The default value of the parameter"))
                .setValue(parameter.defaultValue())
                .setValueType("EXPRESSION")
                .setValueTypeConstraint("string")
                .setPlaceholder(parameter.defaultValue())
                .setEnabled(true)
                .setEditable(true)
                .setType(true)
                .setOptional(true)
                .setAdvanced(false);

        Parameter.Builder parameterBuilder = new Parameter.Builder();
        parameterBuilder
                .metadata(new MetaData(parameter.name(), parameter.description()))
                .kind(parameter.kind())
                .type(parameterType.build())
                .name(parameterName.build())
                .defaultValue(parameterDefaultValue.build())
                .enabled(true)
                .editable(true)
                .optional(true)
                .advanced(false)
                .httpParamType(null);

        return parameterBuilder.build();
    }

    public static Function getInitFunction() {
        Value.ValueBuilder functionName = new Value.ValueBuilder();
        functionName
                .setMetadata(new MetaData("Init Function", "The init function"))
                .setCodedata(new Codedata("FUNCTION_NAME"))
                .setValue("init")
                .setValueType("IDENTIFIER")
                .setValueTypeConstraint("string")
                .setPlaceholder("init")
                .setEnabled(true)
                .setEditable(false)
                .setType(false)
                .setOptional(false)
                .setAdvanced(false);

        Value.ValueBuilder functionReturnType = new Value.ValueBuilder();
        functionReturnType
                .setMetadata(new MetaData("Return Type", "The return type of the function"))
                .setValue("error?")
                .setValueType("TYPE")
                .setValueTypeConstraint("string")
                .setPlaceholder("error?")
                .setEnabled(true)
                .setEditable(true)
                .setType(true)
                .setOptional(true)
                .setAdvanced(false);

        Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
        functionBuilder
                .setMetadata(new MetaData("Init", "The Init function of the service"))
                .setKind("DEFAULT")
                .setEnabled(true)
                .setOptional(false)
                .setEditable(true)
                .setName(functionName.build())
                .setReturnType(new FunctionReturnType(functionReturnType.build()));

        return functionBuilder.build();
    }

    private static Value getTypeDescriptorProperty(ServiceDeclaration template, int packageId) {
        List<String> serviceTypes = ServiceDatabaseManager.getInstance().getServiceTypes(packageId);
        String value = "";
        if (serviceTypes.size() == 1) {
            value = serviceTypes.getFirst();
        }
        List<String> items = new ArrayList<>();
        items.add("");
        items.addAll(serviceTypes);

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.typeDescriptorLabel(), template.typeDescriptorDescription()))
                .setCodedata(new Codedata("SERVICE_TYPE"))
                .setValue(value)
                .setItems(items)
                .setValueType("SINGLE_SELECT")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.typeDescriptorDefaultValue())
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(template.optionalTypeDescriptor() == 0)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getStringLiteralProperty(String value) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData("String Literal", "The string literal of the service"))
                .setCodedata(new Codedata("STRING_LITERAL"))
                .setValue(value)
                .setValues(new ArrayList<>())
                .setValueType("EXPRESSION")
                .setValueTypeConstraint("string")
                .setPlaceholder("\"/path\"")
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    private static Value getStringLiteral(ServiceDeclaration template) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.stringLiteralLabel(), template.stringLiteralDescription()))
                .setCodedata(new Codedata("STRING_LITERAL"))
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType("EXPRESSION")
                .setValueTypeConstraint("string")
                .setPlaceholder(template.stringLiteralDefaultValue())
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    public static Value getBasePathProperty(String value) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData("Base Path", "The base path of the service"))
                .setCodedata(new Codedata("SERVICE_BASE_PATH"))
                .setValue(value)
                .setValues(new ArrayList<>())
                .setValueType("EXPRESSION")
                .setValueTypeConstraint("string")
                .setPlaceholder("/")
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    private static Value getBasePathProperty(ServiceDeclaration template) {
        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(new MetaData(template.absoluteResourcePathLabel(),
                        template.absoluteResourcePathDescription()))
                .setCodedata(new Codedata("SERVICE_BASE_PATH"))
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType("EXPRESSION") // introduce a new type for validation purposes
                .setValueTypeConstraint("string")
                .setPlaceholder(template.absoluteResourcePathDefaultValue())
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(false);

        return valueBuilder.build();
    }

    private static Value getListenersProperty(String protocol, String valueType) {
        boolean isMultiple = valueType.equals("MULTIPLE_SELECT");
        MetaData metaData = isMultiple ?
                new MetaData("Listeners", "The Listeners to be bound with the service")
                : new MetaData("Listener", "The Listener to be bound with the service");

        Value.ValueBuilder valueBuilder = new Value.ValueBuilder();
        valueBuilder
                .setMetadata(metaData)
                .setCodedata(new Codedata("LISTENER"))
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType(valueType)
                .setValueTypeConstraint(protocol+ ":" + "Listener")
                .setPlaceholder("")
                .setOptional(false)
                .setAdvanced(false)
                .setEnabled(true)
                .setEditable(true)
                .setType(false)
                .setAddNewButton(isMultiple);

        return valueBuilder.build();
    }

    public static String getProtocol(String moduleName) {
        String[] split = moduleName.split("\\.");
        return split[split.length - 1];
    }


    public static Optional<Service> getHttpService() {
        InputStream resourceStream = ServiceModelUtils.class.getClassLoader()
                .getResourceAsStream("services/http.json");
        if (resourceStream == null) {
            return Optional.empty();
        }

        try (JsonReader reader = new JsonReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8))) {
            return Optional.of(new Gson().fromJson(reader, Service.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
