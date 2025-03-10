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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDefinitionSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DefaultValueGeneratorUtil;
import io.ballerina.modelgenerator.commons.PackageUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
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
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    private static final Logger LOGGER = Logger.getLogger(TriggerTemplateGenerator.class.getName());
    private static final Type propertyMapType = new TypeToken<Map<String,
            TriggerTemplateGenerator.TriggerProperty>>() { }.getType();

    public static void main(String[] args) {
        BuildProject buildProject = PackageUtil.getSampleProject();

        Map<String, TriggerProperty> newTriggerProperties = readGeneratedTriggerList();
        for (Map.Entry<String, TriggerProperty> trigger : newTriggerProperties.entrySet()) {
            String id = trigger.getKey();
            TriggerProperty triggerProperty = trigger.getValue();
            String org = triggerProperty.orgName();
            PackageMetadataInfo packageMetadataInfo = new PackageMetadataInfo(triggerProperty.packageName(),
                    triggerProperty.version());
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

            String[] parts = packageMetadataInfo.name().split("\\.");
            String moduleName = parts[parts.length - 1];
            String formattedModuleName = upperCaseFirstLetter(moduleName);

            List<String> serviceNames = new ArrayList<>();
            Map<String, List<Function>> serviceFunctions = new HashMap<>();
            AtomicReference<String> defaultFunctionName = new AtomicReference<>("");
            for (Symbol moduleSymbol : semanticModel.moduleSymbols()) {
                if (moduleSymbol instanceof ClassSymbol classSymbol
                        && classSymbol.nameEquals("Listener")) {
                    handleListenerClass(classSymbol, semanticModel, resolvedPackage, formattedModuleName, moduleName,
                            packageMetadataInfo, org, id);
                }

                if (moduleSymbol instanceof TypeDefinitionSymbol typeDefinitionSymbol) {
                    TypeSymbol typeSymbol = typeDefinitionSymbol.typeDescriptor();
                    if (typeSymbol instanceof ObjectTypeSymbol objectTypeSymbol
                            && objectTypeSymbol.qualifiers().contains(Qualifier.SERVICE)) {
                        handleServiceType(typeDefinitionSymbol, objectTypeSymbol, serviceNames,
                                semanticModel, defaultFunctionName, serviceFunctions);
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
                    .setValueType("MULTIPLE_SELECT")
                    .setValueTypeConstraint(moduleName + ":Listener")
                    .setType(false)
                    .setPlaceholder("")
                    .setOptional(false)
                    .setAdvanced(false)
                    .setItems(new ArrayList<>());

            String serviceTypeConstrain = String.join("|",
                    serviceNames.stream().map(name -> moduleName + ":" + name).toList());

            List<String> items = new ArrayList<>();
            items.add("");
            items.addAll(serviceNames);
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
                    .setItems(items);

            Map<String, Value> properties = new LinkedHashMap<>();
            properties.put("listener", listener.build());
            properties.put("serviceType", serviceType.build());

            Service.ServiceModelBuilder builder = new Service.ServiceModelBuilder();
            Service service = builder
                    .setId(id)
                    .setName(formattedModuleName + " Channel")
                    .setType("event")
                    .setDisplayName(formattedModuleName)
                    .setDescription("")
                    .setModuleName(packageMetadataInfo.name())
                    .setOrgName(org)
                    .setListenerProtocol(moduleName.toLowerCase(Locale.ROOT))
                    .setPackageName(packageMetadataInfo.name())
                    .setVersion(packageMetadataInfo.version())
                    .setIcon(CommonUtils.generateIcon(org, packageMetadataInfo.name(), packageMetadataInfo.version()))
                    .setDisplayAnnotation(
                            new DisplayAnnotation(formattedModuleName, ""))
                    .setProperties(properties)
                    .build();

            try {
                Path resourcesPath = getFullPathForResources();
                String resourcesDir = resourcesPath.resolve("services").toString();
                String fileName = packageMetadataInfo.name() + ".json";
                writeJsonToFile(new Gson().toJsonTree(service), resourcesDir, fileName);
                for (Map.Entry<String, List<Function>> entry : serviceFunctions.entrySet()) {
                    String serviceName = entry.getKey();
                    List<Function> functions = entry.getValue();
                    service.getServiceType().setValue(serviceName);
                    service.setFunctions(functions);
                    fileName = moduleName.toLowerCase(Locale.ROOT) + "."
                            + serviceName.toLowerCase(Locale.ROOT) + ".json";
                    writeJsonToFile(new Gson().toJsonTree(service), resourcesDir, fileName);
                }
            } catch (IOException e) {
                LOGGER.severe("Error writing service to file: " + e.getMessage());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static Path getFullPathForResources() throws URISyntaxException {
        // Get ProtectionDomain
        ProtectionDomain protectionDomain = TriggerTemplateGenerator.class.getProtectionDomain();
        if (protectionDomain == null) {
            throw new IllegalStateException("Protection domain is null.");
        }

        // Get CodeSource
        CodeSource codeSource = protectionDomain.getCodeSource();
        if (codeSource == null) {
            throw new IllegalStateException("Code source is null.");
        }

        // Get Location URL
        URL location = codeSource.getLocation();
        if (location == null) {
            throw new IllegalStateException("Location URL is null.");
        }

        // Convert URL to URI
        URI uri = location.toURI();

        // Convert URI to Path
        Path path = Paths.get(uri);

        // Traverse up four parent directories with checks
        for (int i = 0; i < 4; i++) {
            path = path.getParent();
            if (path == null) {
                throw new IllegalStateException("Cannot traverse parent directories: path is null at step " + (i + 1));
            }
        }

        // Resolve the target directory
        Path resourcesPath = path.resolve("src/main/resources");
        return resourcesPath;
    }

    private static Map<String, TriggerProperty> readGeneratedTriggerList() {
        InputStream newPropertiesStream = TriggerTemplateGenerator.class.getClassLoader()
                .getResourceAsStream("generated_triggers.json");
        Map<String, TriggerProperty> newTriggerProperties = Map.of();
        if (newPropertiesStream != null) {
            try (JsonReader reader = new JsonReader(new InputStreamReader(newPropertiesStream,
                    StandardCharsets.UTF_8))) {
                newTriggerProperties = new Gson().fromJson(reader, propertyMapType);
            } catch (IOException e) {
                // Ignore
            }
        }
        return newTriggerProperties;
    }

    private record TriggerProperty(String orgName, String packageName, String version) { }

    private static void handleServiceType(TypeDefinitionSymbol typeDefinitionSymbol, ObjectTypeSymbol objectTypeSymbol,
                                          List<String> serviceNames, SemanticModel semanticModel,
                                          AtomicReference<String> defaultFunctionName,
                                          Map<String, List<Function>> serviceFunctions) {
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

    private static void handleListenerClass(ClassSymbol classSymbol, SemanticModel semanticModel,
                                            Package resolvedPackage, String formattedModuleName, String moduleName,
                                            PackageMetadataInfo packageMetadataInfo, String org, String id) {
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
                    Map<String, String> docMap;
                    if (methodSymbol.get().documentation().isPresent()) {
                        docMap = methodSymbol.get().documentation().get().parameterMap();
                    } else {
                        docMap = new HashMap<>();
                    }
                    processParameterSymbol(param, docMap, resolvedPackage, semanticModel, properties);
                }
            }

            Listener.ListenerBuilder listenerBuilder = new Listener.ListenerBuilder();
            listenerBuilder
                    .setId(id)
                    .setName(formattedModuleName + " Listener")
                    .setType("event")
                    .setDisplayName(formattedModuleName)
                    .setDescription("")
                    .setListenerProtocol(moduleName.toLowerCase(Locale.ROOT))
                    .setModuleName(packageMetadataInfo.name())
                    .setOrgName(org)
                    .setPackageName(packageMetadataInfo.name())
                    .setVersion(packageMetadataInfo.version())
                    .setIcon(CommonUtils.generateIcon(org, packageMetadataInfo.name(), packageMetadataInfo.version()))
                    .setDisplayAnnotation(
                            new DisplayAnnotation(formattedModuleName, ""))
                    .setProperties(properties);

            try {
                Path resourcesPath = getFullPathForResources();
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

    private static void processParameterSymbol(ParameterSymbol paramSymbol, Map<String, String> documentationMap,
                                               Package resolvedPackage, SemanticModel semanticModel,
                                               Map<String, Value> properties) {
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterKind parameterKind = paramSymbol.paramKind();
        String paramType;
        boolean optional = true;
        String defaultValue;
        TypeSymbol typeSymbol = paramSymbol.typeDescriptor();
        if (parameterKind == ParameterKind.REST) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
            paramType = CommonUtils.getTypeSignature(semanticModel,
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor(), false);
        } else if (parameterKind == ParameterKind.INCLUDED_RECORD) {
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            addIncludedRecordParamsToDb((RecordTypeSymbol) CommonUtils.getRawType(typeSymbol),
                    resolvedPackage, semanticModel, true, new HashMap<>(), properties);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        } else if (parameterKind == ParameterKind.REQUIRED) {
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            optional = false;
        } else {
            Location symbolLocation = paramSymbol.getLocation().get();
            Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            if (document != null) {
                defaultValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, resolvedPackage.packageName().value());
            }
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
        }

        if (parameterKind != ParameterKind.INCLUDED_RECORD) {
            Value.ValueBuilder property = new Value.ValueBuilder();
            property
                    .setMetadata(new MetaData(paramName,
                            paramDescription != null ? paramDescription : ""))
                    .setCodedata(new Codedata("LISTENER_INIT_PARAM", "NAMED"))
                    .setEditable(true)
                    .setEnabled(true)
                    .setValue("")
                    .setValueType("EXPRESSION")
                    .setValueTypeConstraint(paramType)
                    .setPlaceholder(defaultValue)
                    .setOptional(optional)
                    .setAdvanced(false);
            properties.put(paramName, property.build());
        }
    }

    protected static void addIncludedRecordParamsToDb(RecordTypeSymbol recordTypeSymbol, Package resolvedPackage,
                                                      SemanticModel semanticModel, boolean insert,
                                                      Map<String, String> documentationMap,
                                                      Map<String, Value> properties) {
        recordTypeSymbol.typeInclusions().forEach(includedType -> addIncludedRecordParamsToDb(
                ((RecordTypeSymbol) CommonUtils.getRawType(includedType)), resolvedPackage,
                semanticModel, false, documentationMap, properties)
        );
        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol typeSymbol = recordFieldSymbol.typeDescriptor();
            TypeSymbol fieldType = CommonUtil.getRawType(typeSymbol);
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
            String paramDescription = entry.getValue().documentation()
                    .flatMap(Documentation::description).orElse("");
            if (documentationMap.containsKey(paramName) && !paramDescription.isEmpty()) {
                documentationMap.put(paramName, paramDescription);
            } else if (!documentationMap.containsKey(paramName)) {
                documentationMap.put(paramName, paramDescription);
            }
            if (!insert) {
                continue;
            }

            Location symbolLocation = recordFieldSymbol.getLocation().get();
            Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
            String defaultValue;
            if (document != null) {
                defaultValue = getAttributeDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, resolvedPackage.packageName().value());
                if (defaultValue == null) {
                    defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
                }
            } else {
                defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
            }
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            boolean optional = false;
            if (recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue()) {
                optional = true;
            }
            Value.ValueBuilder property = new Value.ValueBuilder();
            property
                    .setMetadata(new MetaData(paramName, paramDescription))
                    .setCodedata(new Codedata("LISTENER_INIT_PARAM", "NAMED"))
                    .setEditable(true)
                    .setEnabled(true)
                    .setValue("")
                    .setValueType("EXPRESSION")
                    .setValueTypeConstraint(paramType)
                    .setPlaceholder(defaultValue)
                    .setOptional(optional)
                    .setAdvanced(optional);
            properties.put(paramName, property.build());
        }
    }

    private static String getAttributeDefaultValue(ModulePartNode rootNode, Location location, String module) {
        NonTerminalNode node = rootNode.findNode(TextRange.from(location.textRange().startOffset(),
                location.textRange().length()));
        if (node.kind() == SyntaxKind.RECORD_FIELD_WITH_DEFAULT_VALUE) {
            RecordFieldWithDefaultValueNode valueNode = (RecordFieldWithDefaultValueNode) node;
            ExpressionNode expression = valueNode.expression();
            if (expression instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
                return module + ":" + simpleNameReferenceNode.name().text();
            } else if (expression instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) {
                return qualifiedNameReferenceNode.modulePrefix().text() + ":" + qualifiedNameReferenceNode.identifier()
                        .text();
            } else {
                return expression.toSourceCode();
            }
        }
        return null;
    }

    private static String getParamDefaultValue(ModulePartNode rootNode, Location location, String module) {
        NonTerminalNode node = rootNode.findNode(TextRange.from(location.textRange().startOffset(),
                location.textRange().length()));
        if (node.kind() == SyntaxKind.DEFAULTABLE_PARAM) {
            DefaultableParameterNode valueNode = (DefaultableParameterNode) node;
            ExpressionNode expression = (ExpressionNode) valueNode.expression();
            if (expression instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
                return module + ":" + simpleNameReferenceNode.name().text();
            } else if (expression instanceof QualifiedNameReferenceNode qualifiedNameReferenceNode) {
                return qualifiedNameReferenceNode.modulePrefix().text() + ":" + qualifiedNameReferenceNode.identifier()
                        .text();
            } else if (expression instanceof MappingConstructorExpressionNode) {
                return "{}";
            } else {
                return expression.toSourceCode();
            }
        }
        return null;
    }

    public static Document findDocument(Package pkg, String path) {
        Project project = pkg.project();
        Module defaultModule = pkg.getDefaultModule();
        String module = pkg.packageName().value();
        Path docPath = project.sourceRoot().resolve("modules").resolve(module).resolve(path);
        try {
            DocumentId documentId = project.documentId(docPath);
            return defaultModule.document(documentId);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    record PackageMetadataInfo(String name, String version) { }

    public static String upperCaseFirstLetter(String value) {
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
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
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(jsonElement, writer);
        }
    }
}
