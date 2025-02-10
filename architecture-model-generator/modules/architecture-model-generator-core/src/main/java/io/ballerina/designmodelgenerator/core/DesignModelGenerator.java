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

package io.ballerina.designmodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ObjectTypeSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.designmodelgenerator.core.model.Automation;
import io.ballerina.designmodelgenerator.core.model.Connection;
import io.ballerina.designmodelgenerator.core.model.DesignModel;
import io.ballerina.designmodelgenerator.core.model.Function;
import io.ballerina.designmodelgenerator.core.model.Listener;
import io.ballerina.designmodelgenerator.core.model.Location;
import io.ballerina.designmodelgenerator.core.model.ResourceFunction;
import io.ballerina.designmodelgenerator.core.model.Service;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generate the design model for the default package.
 *
 * @since 2.0.0
 */
public class DesignModelGenerator {

    private final SemanticModel semanticModel;
    private final Module defaultModule;
    private final Path rootPath;
    public static final String MAIN_FUNCTION_NAME = "main";
    private static final String AUTOMATION = "automation";
    private static final String SERVICE = "Service";
    private final Map<String, ModulePartNode> documentMap;

    public DesignModelGenerator(Package ballerinaPackage) {
        this.defaultModule = ballerinaPackage.getDefaultModule();
        this.semanticModel = this.defaultModule.getCompilation().getSemanticModel();
        this.rootPath = ballerinaPackage.project().sourceRoot();
        this.documentMap = new HashMap<>();
        this.defaultModule.documentIds().forEach(documentId -> {
            Document document = this.defaultModule.document(documentId);
            documentMap.put(document.name(), document.syntaxTree().rootNode());
        });
    }

    public DesignModel generate() {
        IntermediateModel intermediateModel = new IntermediateModel();
        this.populateModuleLevelConnections(intermediateModel);
        ConnectionFinder connectionFinder = new ConnectionFinder(semanticModel, rootPath, documentMap,
                intermediateModel);
        this.defaultModule.documentIds().forEach(d -> {
            ModulePartNode rootNode =  this.defaultModule.document(d).syntaxTree().rootNode();
            CodeAnalyzer codeAnalyzer = new CodeAnalyzer(semanticModel, intermediateModel, rootPath, connectionFinder);
            codeAnalyzer.visit(rootNode);
        });

        DesignModel.DesignModelBuilder builder = new DesignModel.DesignModelBuilder();

        if (intermediateModel.functionModelMap.containsKey(MAIN_FUNCTION_NAME)) {
            IntermediateModel.FunctionModel main = intermediateModel.functionModelMap.get(MAIN_FUNCTION_NAME);
            buildConnectionGraph(intermediateModel, main);
            builder.setAutomation(new Automation(AUTOMATION, main.displayName, "Z", main.location,
                    main.allDependentConnections.stream().toList()));
        }

        for (Map.Entry<String, IntermediateModel.ServiceModel> serviceEntry :
                intermediateModel.serviceModelMap.entrySet()) {
            IntermediateModel.ServiceModel serviceModel = serviceEntry.getValue();
            Set<String> connections = new HashSet<>();
            List<Function> functions = new ArrayList<>();
            serviceModel.otherFunctions.forEach(otherFunction -> {
                buildConnectionGraph(intermediateModel, otherFunction);
                functions.add(new Function(otherFunction.name, otherFunction.location,
                        otherFunction.allDependentConnections));
                connections.addAll(otherFunction.allDependentConnections);
            });

            List<Function> remoteFunctions = new ArrayList<>();
            serviceModel.remoteFunctions.forEach(remoteFunction -> {
                buildConnectionGraph(intermediateModel, remoteFunction);
                remoteFunctions.add(new Function(remoteFunction.name, remoteFunction.location,
                        remoteFunction.allDependentConnections));
                connections.addAll(remoteFunction.allDependentConnections);
            });

            List<ResourceFunction> resourceFunctions = new ArrayList<>();
            serviceModel.resourceFunctions.forEach(resourceFunction -> {
                buildConnectionGraph(intermediateModel, resourceFunction);
                resourceFunctions.add(new ResourceFunction(resourceFunction.name, resourceFunction.path,
                        resourceFunction.location, resourceFunction.allDependentConnections));
                connections.addAll(resourceFunction.allDependentConnections);
            });
            List<Listener> allAttachedListeners = serviceModel.anonListeners;
            for (String listener : serviceModel.namedListeners) {
                allAttachedListeners.add(intermediateModel.listeners.get(listener));
            }

            Service service = new Service(serviceModel.displayName, serviceModel.absolutePath, serviceModel.location,
                    serviceModel.sortText,
                    connections.stream().toList(), functions, remoteFunctions, resourceFunctions);
            int size = allAttachedListeners.size();
            if (size > 0) {
                Listener listener = allAttachedListeners.get(0);
                service.setIcon(listener.getIcon());
                service.setType(getServiceType(listener.getType()));
                for (int i = 0; i < size; i++) {
                    listener = allAttachedListeners.get(i);
                    listener.getAttachedServices().add(service.getUuid());
                    service.addAttachedListener(listener.getUuid());
                }
            }
            builder.addService(service);
        }
        return builder
                .setListeners(intermediateModel.listeners.values().stream().toList())
                .setConnections(intermediateModel.connectionMap.values().stream().toList())
                .build();
    }

    private void populateModuleLevelConnections(IntermediateModel intermediateModel) {
        for (Symbol symbol : this.semanticModel.moduleSymbols()) {
            if (symbol instanceof VariableSymbol variableSymbol) {
                TypeSymbol typeSymbol = CommonUtils.getRawType(variableSymbol.typeDescriptor());
                if (typeSymbol instanceof ObjectTypeSymbol objectTypeSymbol) {
                    if (objectTypeSymbol.qualifiers().contains(Qualifier.CLIENT)) {
                        LineRange lineRange = variableSymbol.getLocation().get().lineRange();
                        String sortText = lineRange.fileName() + lineRange.startLine().line();
                        String icon =  CommonUtils.generateIcon(variableSymbol.typeDescriptor());
                        Connection connection = new Connection(variableSymbol.getName().get(), sortText,
                                getLocation(lineRange), Connection.Scope.GLOBAL, icon, true);
                        intermediateModel.connectionMap.put(
                                String.valueOf(variableSymbol.getLocation().get().hashCode()), connection);
                    }
                }
            }
        }
    }

    private void buildConnectionGraph(IntermediateModel intermediateModel,
                                      IntermediateModel.FunctionModel functionModel) {
        Set<String> connections = new HashSet<>();
        if (!functionModel.visited && !functionModel.analyzed) {
            functionModel.visited = true;
            functionModel.dependentFuncs.forEach(dependentFunc -> {
                IntermediateModel.FunctionModel dependentFunctionModel = intermediateModel.functionModelMap
                        .get(dependentFunc);
                if (!dependentFunctionModel.analyzed) {
                    buildConnectionGraph(intermediateModel, dependentFunctionModel);
                }
                connections.addAll(dependentFunctionModel.allDependentConnections);
            });
        }
        functionModel.visited = true;
        functionModel.allDependentConnections.addAll(functionModel.connections);
        functionModel.allDependentConnections.addAll(connections);
        functionModel.analyzed = true;
    }

    public Location getLocation(LineRange lineRange) {
        Path filePath = rootPath.resolve(lineRange.fileName());
        return new Location(filePath.toAbsolutePath().toString(), lineRange.startLine(),
                lineRange.endLine());
    }

    public String getServiceType(String listenerType) {
        return listenerType.split(SyntaxKind.COLON_TOKEN.stringValue())[0]
                + SyntaxKind.COLON_TOKEN.stringValue() + SERVICE;
    }
}
