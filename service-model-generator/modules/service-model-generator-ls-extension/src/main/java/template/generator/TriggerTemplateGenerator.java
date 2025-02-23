/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package template.generator;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.DisplayAnnotation;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.FunctionReturnType;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.MetaData;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Generates the trigger template for a given package.
 *
 * @since 2.0.0
 */
public class TriggerTemplateGenerator {

    static Logger LOGGER = Logger.getLogger(TriggerTemplateGenerator.class.getName());

    public static void main(String[] args) {
        BuildProject buildProject = PackageUtil.getSampleProject();
        String org = "ballerinax";
        PackageMetadataInfo packageMetadataInfo = new PackageMetadataInfo("trigger.salesforce", "0.10.0");

        Package resolvedPackage;
        try {
            resolvedPackage = Objects.requireNonNull(PackageUtil.getModulePackage(buildProject, org,
                    packageMetadataInfo.name(), packageMetadataInfo.version())).orElseThrow();
        } catch (Throwable e) {
            LOGGER.severe("Error resolving package: " + packageMetadataInfo.name() + e.getMessage());
            return;
        }
        PackageDescriptor descriptor = resolvedPackage.descriptor();

        LOGGER.info("Processing package: " + descriptor.name().value());

        SemanticModel semanticModel;
        try {
            semanticModel = resolvedPackage.getCompilation()
                    .getSemanticModel(resolvedPackage.getDefaultModule().moduleId());
        } catch (Exception e) {
            LOGGER.severe("Error reading semantic model: " + e.getMessage());
            return;
        }

        String moduleName = packageMetadataInfo.name().split("\\.")[1];
        String formattedModuleName = upperCaseFirstLetter(moduleName);

        List<String> serviceNames = new ArrayList<>();
        Map<String, List<Function>> serviceFunctions = new HashMap<>();
        AtomicReference<String> defaultFunctionName = new AtomicReference<>("");
        for (Symbol moduleSymbol : semanticModel.moduleSymbols()) {
            if (moduleSymbol instanceof ClassSymbol classSymbol
                    && classSymbol.nameEquals("Listener")) {
                Value.ValueBuilder nameProperty = new Value.ValueBuilder();
                nameProperty
                        .setMetadata(new MetaData("Name", "The name of the listener"))
                        .setEnabled(true)
                        .setEditable(true)
                        .setValue("")
                        .setValueType("IDENTIFIER")
                        .setOptional(false)
                        .setAdvanced(false)
                        .setType(false)
                        .setCodedata(new Codedata("LISTENER_VAR_NAME"));

                Map<String, Value> properties = new LinkedHashMap<>();
                properties.put("name", nameProperty.build());

                Optional<MethodSymbol> methodSymbol = classSymbol.initMethod();
                if (methodSymbol.isPresent()) {
                    Optional<List<ParameterSymbol>> params = methodSymbol.get().typeDescriptor().params();
                    if (params.isPresent()) {
                        for (ParameterSymbol param : params.get()) {
                            Value.ValueBuilder property = new Value.ValueBuilder();
                            property
                                    .setMetadata(new MetaData("", ""))
                                    .setCodedata(new Codedata("LISTENER_INIT_PARAM", "NAMED"))
                                    .setEditable(true)
                                    .setEnabled(true)
                                    .setValue("")
                                    .setValueType("EXPRESSION")
                                    .setValueTypeConstraint(CommonUtils.getTypeSignature(semanticModel,
                                            param.typeDescriptor(), false))
                                    .setPlaceholder("")
                                    .setOptional(false)
                                    .setAdvanced(false);
                            properties.put(param.getName().orElse("param"), property.build());
                        }
                    }

                    Listener.ListenerBuilder listenerBuilder = new Listener.ListenerBuilder();
                    listenerBuilder
                            .setId("1")
                            .setName(formattedModuleName + " Listener")
                            .setType("event")
                            .setDisplayName(formattedModuleName)
                            .setDescription("")
                            .setListenerProtocol(moduleName.toLowerCase())
                            .setModuleName(packageMetadataInfo.name())
                            .setOrgName(org)
                            .setPackageName(packageMetadataInfo.name())
                            .setVersion(packageMetadataInfo.version())
                            .setIcon(CommonUtils.generateIcon(org, packageMetadataInfo.name(), packageMetadataInfo.version()))
                            .setDisplayAnnotation(
                                    new DisplayAnnotation(formattedModuleName, ""))
                            .setProperties(properties);

                    try {
                        Path resourcesPath = Paths.get(TriggerTemplateGenerator.class.getProtectionDomain()
                                        .getCodeSource().getLocation().toURI()).getParent().
                                getParent().getParent().getParent().resolve("src/main/resources");
                        String resourcesDir = resourcesPath.resolve("listeners").toString();
                        String fileName = packageMetadataInfo.name() + ".json";
                        writeJsonToFile(new Gson().toJsonTree(listenerBuilder.build()), resourcesDir, fileName);
                    } catch (IOException e) {
                        LOGGER.severe("Error writing service to file: " + e.getMessage());
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (moduleSymbol instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                TypeSymbol typeSymbol = typeDefinitionSymbol.typeDescriptor();
                if (typeSymbol instanceof ObjectTypeSymbol objectTypeSymbol
                        && objectTypeSymbol.qualifiers().contains(Qualifier.SERVICE)) {
                    serviceNames.add(typeDefinitionSymbol.getName().get());
                    List<Function> functions = new ArrayList<>();
                    objectTypeSymbol.methods().forEach((methodName, methodSymbol) -> {
                        if (methodSymbol.qualifiers().contains(Qualifier.REMOTE)) {
                            Optional<Documentation> documentation = methodSymbol.documentation();
                            String methodDescription = "";
                            Map<String, String> paramDocMap = new HashMap<>();
                            if (documentation.isPresent()) {
                                methodDescription = documentation.get().description().orElse("");
                                paramDocMap = documentation.get().parameterMap();
                            }

                            FunctionTypeSymbol functionTypeSymbol = methodSymbol.typeDescriptor();
                            Optional<List<ParameterSymbol>> params = functionTypeSymbol.params();
                            List<Parameter> parameterList = new ArrayList<>();
                            if (params.isPresent()) {
                                for (ParameterSymbol param : params.get()) {
                                    Value.ValueBuilder paramType = new Value.ValueBuilder();
                                    paramType
                                            .setEnabled(true)
                                            .setEditable(false)
                                            .setValue(CommonUtils.getTypeSignature(semanticModel,
                                                    param.typeDescriptor(), false))
                                            .setValueType("TYPE")
                                            .setType(true)
                                            .setOptional(false)
                                            .setAdvanced(false);

                                    String name = param.getName().orElse("param");

                                    Value.ValueBuilder paramName = new Value.ValueBuilder();
                                    paramName
                                            .setEnabled(true)
                                            .setEditable(true)
                                            .setValue(name)
                                            .setValueType("IDENTIFIER")
                                            .setType(false)
                                            .setOptional(false)
                                            .setAdvanced(false);

                                    Parameter parameter = new Parameter();
                                    parameter.setMetadata(new MetaData(name, paramDocMap.get(name) == null ? "" :
                                            paramDocMap.get(name)));
                                    parameter.setKind(param.paramKind().name());
                                    parameter.setEnabled(true);
                                    parameter.setEnabled(true);
                                    parameter.setOptional(false);
                                    parameter.setType(paramType.build());
                                    parameter.setName(paramName.build());
                                    parameterList.add(parameter);
                                }
                            }

                            Function.FunctionBuilder functionBuilder = new Function.FunctionBuilder();
                            Value.ValueBuilder name = new Value.ValueBuilder();
                            name
                                    .setEnabled(true)
                                    .setEditable(false)
                                    .setValue(methodName)
                                    .setValueType("IDENTIFIER")
                                    .setOptional(false)
                                    .setAdvanced(false)
                                    .setType(false)
                                    .setItems(new ArrayList<>());

                            Value.ValueBuilder returnType = new Value.ValueBuilder();
                            returnType
                                    .setEnabled(true)
                                    .setEditable(false)
                                    .setValue(CommonUtils.getTypeSignature(semanticModel,
                                            functionTypeSymbol.returnTypeDescriptor().get(), false))
                                    .setValueType("TYPE")
                                    .setOptional(false)
                                    .setAdvanced(false)
                                    .setType(true);

                            functionBuilder
                                    .setMetadata(new MetaData(methodName, methodDescription))
                                    .setKind("REMOTE")
                                    .setName(name.build())
                                    .setEnabled(true)
                                    .setEditable(true)
                                    .setOptional(false)
                                    .setParameters(parameterList)
                                    .setReturnType(new FunctionReturnType(returnType.build()));

                            functions.add(functionBuilder.build());
                            defaultFunctionName.set(methodName);
                        }
                    });
                    serviceFunctions.put(typeDefinitionSymbol.getName().get(), functions);
                }
            }
        }

        Value.ValueBuilder listener = new Value.ValueBuilder();
        listener
                .setMetadata(new MetaData("Listener",
                        "The Listeners to be bound with the service"))
                .setCodedata(new Codedata("LISTENER"))
                .setEnabled(true)
                .setEditable(false)
                .setValue("")
                .setValues(new ArrayList<>())
                .setValueType("MULTIPLE_SELECT")
                .setValueTypeConstraint(moduleName + ":Listener")
                .setType(false)
                .setPlaceholder("")
                .setOptional(false)
                .setAdvanced(false)
                .setAddNewButton(true)
                .setItems(new ArrayList<>());

        String serviceTypeConstrain = String.join("|",
                serviceNames.stream().map(name -> moduleName + ":" + name).toList());

        Value.ValueBuilder serviceType = new Value.ValueBuilder();
        serviceType
                .setMetadata(new MetaData("Channel", "The channel name"))
                .setCodedata(new Codedata("SERVICE_TYPE"))
                .setEnabled(true)
                .setEditable(true)
                .setEditable(true)
                .setValue(defaultFunctionName.get())
                .setValueType("SINGLE_SELECT")
                .setValueTypeConstraint(serviceTypeConstrain)
                .setType(true)
                .setPlaceholder("")
                .setOptional(false)
                .setAdvanced(false)
                .setItems(serviceNames);

        Map<String, Value> properties = new LinkedHashMap<>();
        properties.put("listener", listener.build());
        properties.put("serviceType", serviceType.build());

        Service.ServiceModelBuilder builder = new Service.ServiceModelBuilder();
        Service service = builder
                .setId("1")
                .setName(formattedModuleName + " Channel")
                .setType("event")
                .setDisplayName(formattedModuleName)
                .setDescription("")
                .setModuleName(packageMetadataInfo.name())
                .setOrgName(org)
                .setListenerProtocol(moduleName.toLowerCase())
                .setPackageName(packageMetadataInfo.name())
                .setVersion(packageMetadataInfo.version())
                .setIcon(CommonUtils.generateIcon(org, packageMetadataInfo.name(), packageMetadataInfo.version()))
                .setDisplayAnnotation(
                        new DisplayAnnotation(formattedModuleName, ""))
                .setProperties(properties)
                .build();

        try {
            Path resourcesPath = Paths.get(TriggerTemplateGenerator.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()).getParent().
                    getParent().getParent().getParent().resolve("src/main/resources");
            String resourcesDir = resourcesPath.resolve("services").toString();
            String fileName = packageMetadataInfo.name() + ".json";
            writeJsonToFile(new Gson().toJsonTree(service), resourcesDir, fileName);
            for (Map.Entry<String, List<Function>> entry : serviceFunctions.entrySet()) {
                String serviceName = entry.getKey();
                List<Function> functions = entry.getValue();
                service.getServiceType().setValue(serviceName);
                service.setFunctions(functions);
                fileName = moduleName.toLowerCase() + "." + serviceName.toLowerCase() + ".json";
                writeJsonToFile(new Gson().toJsonTree(service), resourcesDir, fileName);
            }
        } catch (IOException e) {
            LOGGER.severe("Error writing service to file: " + e.getMessage());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    record PackageMetadataInfo(String name, String version) { }

    public static String upperCaseFirstLetter(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    public static void writeJsonToFile(JsonElement jsonElement, String resourcesDir, String fileName)
            throws IOException {
        Path directoryPath = Paths.get(resourcesDir);

        // Create the directory if it doesn't exist
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }

        // Define the full file path
        Path filePath = directoryPath.resolve(fileName);

        // Write JSON to file with UTF-8 encoding
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            gson.toJson(jsonElement, writer);
        }
    }
}
