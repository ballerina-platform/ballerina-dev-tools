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

package io.ballerina.modelgenerator.commons;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.DefaultableParameterNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.RecordFieldWithDefaultValueNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory class to create FunctionResult instances from function symbols.
 *
 * @since 2.0.0
 */
public class FunctionFactory {

    /**
     * Creates a FunctionResult from a FunctionSymbol.
     * 
     * @param semanticModel The semantic model
     * @param functionSymbol The function symbol
     * @param documentable The documentable source
     * @param packageName The package name
     * @param functionType The function type
     * @param errorTypeSymbol The error type symbol
     * @param resolvedPackage The resolved package
     * @return The FunctionResult
     */
    public static FunctionResult createFunction(SemanticModel semanticModel,
                                             FunctionSymbol functionSymbol,
                                             Documentable documentable,
                                             String packageName,
                                             FunctionResult.Kind functionType,
                                             TypeSymbol errorTypeSymbol,
                                             Package resolvedPackage) {

        String name = functionSymbol.getName().orElse("");
        String description = getDescription(documentable);

        // Get the return type
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        String returnType = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> functionSymbol.nameEquals("init") ? getClientType(packageName)
                        : CommonUtils.getTypeSignature(semanticModel, returnTypeDesc, true))
                .orElse("");

        // Check if return type contains error
        boolean returnError = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol) ? true : false)
                .orElse(false);

        // Get resource path for resource functions 
        String resourcePath = "";
        if (functionType == FunctionResult.Kind.RESOURCE) {
            // Note: Implement resource path extraction if needed
            // This would use ParamUtils.buildResourcePathTemplate() from the original code
        }

        return new FunctionResult(
            0, // functionId is not needed since we're not using DB
            name,
            description,
            returnType,
            resolvedPackage.packageName().value(),
            resolvedPackage.descriptor().org().value(),
            resolvedPackage.descriptor().version().value().toString(),
            resourcePath,
            functionType,
            returnError,
            false // inferredReturnType can be added as needed
        );
    }

    /**
     * Creates a FunctionResult from a FunctionSymbol with parameters.
     */
    public static FunctionResult createFunctionWithParams(SemanticModel semanticModel,
                                                       FunctionSymbol functionSymbol,
                                                       Documentable documentable,
                                                       String packageName,
                                                       FunctionResult.Kind functionType,
                                                       TypeSymbol errorTypeSymbol,
                                                       Package resolvedPackage) {
        
        FunctionResult functionResult = createFunction(semanticModel, functionSymbol, documentable, 
                packageName, functionType, errorTypeSymbol, resolvedPackage);

        // Get documentation map for parameters
        Map<String, String> documentationMap = functionSymbol.documentation()
                .map(Documentation::parameterMap)
                .orElse(Map.of());

        List<ParameterResult> parameters = new ArrayList<>();
        ModuleInfo defaultModuleInfo = ModuleInfo.from(resolvedPackage.getDefaultModule().descriptor());

        // Process resource path parameters if needed
        if (functionType == FunctionResult.Kind.RESOURCE) {
            ParamUtils.ResourcePathTemplate resourcePathTemplate = 
                    ParamUtils.buildResourcePathTemplate(semanticModel, functionSymbol, errorTypeSymbol);
            if (resourcePathTemplate != null) {
                parameters.addAll(processResourcePathParams(resourcePathTemplate.pathParams()));
            }
        }

        // Process function parameters
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        functionTypeSymbol.params().ifPresent(paramList -> 
            paramList.forEach(paramSymbol -> 
                parameters.addAll(processParameterSymbol(paramSymbol, documentationMap, resolvedPackage,
                        defaultModuleInfo, semanticModel, null))
            )
        );

        // Process rest parameter if present
        functionTypeSymbol.restParam().ifPresent(paramSymbol ->
            parameters.addAll(processParameterSymbol(paramSymbol, documentationMap, resolvedPackage,
                    defaultModuleInfo, semanticModel, null))
        );

        functionResult.setParameters(parameters);
        return functionResult;
    }

    private static List<ParameterResult> processParameterSymbol(ParameterSymbol paramSymbol,
                                                        Map<String, String> documentationMap,
                                                        Package resolvedPackage,
                                                        ModuleInfo defaultModuleInfo,
                                                        SemanticModel semanticModel,
                                                        ParamForTypeInfer paramForTypeInfer) {
        List<ParameterResult> parameters = new ArrayList<>();
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterResult.Kind parameterKind = getParameterKind(paramSymbol.paramKind());
        
        TypeSymbol typeSymbol = paramSymbol.typeDescriptor();
        String paramType;
        boolean optional = true;
        String defaultValue;
        String importStatements = CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null);

        if (parameterKind == ParameterResult.Kind.REST_PARAMETER) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
            paramType = CommonUtils.getTypeSignature(semanticModel,
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor(), false);
        } else if (parameterKind == ParameterResult.Kind.INCLUDED_RECORD) {
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            List<ParameterResult> includedParams = processIncludedRecordParams(
                    (RecordTypeSymbol) CommonUtils.getRawType(typeSymbol),
                    resolvedPackage, defaultModuleInfo, semanticModel, documentationMap);
            parameters.addAll(includedParams);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        } else if (parameterKind == ParameterResult.Kind.REQUIRED) {
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            optional = false;
        } else {
            if (paramForTypeInfer != null && paramForTypeInfer.paramName().equals(paramName)) {
                defaultValue = paramForTypeInfer.defaultValue();
                paramType = paramForTypeInfer.type();
                parameters.add(ParameterResult.from(paramName, paramDescription, paramType, defaultValue,
                        ParameterResult.Kind.PARAM_FOR_TYPE_INFER, optional, importStatements));
                return parameters;
            }

            Location symbolLocation = paramSymbol.getLocation().get();
            Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            if (document != null) {
                String derivedValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                        symbolLocation, resolvedPackage.packageName().value());
                if (derivedValue != null) {
                    defaultValue = derivedValue;
                }
            }
            paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
        }

        parameters.add(ParameterResult.from(paramName, paramDescription, paramType, defaultValue,
                parameterKind, optional, importStatements));
        return parameters;
    }

    private static List<ParameterResult> processIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                                   Package resolvedPackage,
                                                                   ModuleInfo defaultModuleInfo,
                                                                   SemanticModel semanticModel,
                                                                   Map<String, String> documentationMap) {
        List<ParameterResult> parameters = new ArrayList<>();
        
        // Process type inclusions recursively
        recordTypeSymbol.typeInclusions().forEach(includedType ->
            parameters.addAll(processIncludedRecordParams(
                (RecordTypeSymbol) CommonUtils.getRawType(includedType),
                resolvedPackage, defaultModuleInfo, semanticModel, documentationMap))
        );

        // Process record fields
        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol typeSymbol = recordFieldSymbol.typeDescriptor();
            TypeSymbol fieldType = CommonUtils.getRawType(typeSymbol);
            
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }

            String paramName = entry.getKey();
            String paramDescription = recordFieldSymbol.documentation()
                    .flatMap(Documentation::description).orElse("");

            if (documentationMap.containsKey(paramName) && !paramDescription.isEmpty()) {
                documentationMap.put(paramName, paramDescription);
            } else if (!documentationMap.containsKey(paramName)) {
                documentationMap.put(paramName, paramDescription);
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
            boolean optional = recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue();

            parameters.add(ParameterResult.from(paramName, documentationMap.get(paramName),
                    paramType, defaultValue, ParameterResult.Kind.INCLUDED_FIELD, optional,
                    CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null)));
        }

        // Process rest type if present
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            parameters.add(ParameterResult.from("Additional Values", "Capture key value pairs",
                    paramType, defaultValue, ParameterResult.Kind.INCLUDED_RECORD_REST, true,
                    CommonUtils.getImportStatements(typeSymbol, defaultModuleInfo).orElse(null)));
        });

        return parameters;
    }

    private static List<ParameterResult> processResourcePathParams(List<ParameterResult> pathParams) {
        return pathParams.stream()
                .map(param -> ParameterResult.from(param.name(), param.description(),
                        param.type(), param.defaultValue(),
                        ParameterResult.Kind.valueOf(param.kind().name()),
                        param.optional(), null))
                .toList();
    }

    private static ParameterResult.Kind getParameterKind(ParameterKind paramKind) {
        return switch (paramKind) {
            case DEFAULTABLE -> ParameterResult.Kind.DEFAULTABLE;
            case REST -> ParameterResult.Kind.REST_PARAMETER;
            default -> ParameterResult.Kind.REQUIRED;
        };
    }

    private record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }

    /**
     * Gets the kind of function from the given method symbol.
     *
     * @param methodSymbol The method symbol
     * @return The function kind
     */
    public static FunctionResult.Kind getFunctionKind(MethodSymbol methodSymbol) {
        List<Qualifier> qualifiers = methodSymbol.qualifiers();
        if (qualifiers.contains(Qualifier.REMOTE)) {
            return FunctionResult.Kind.REMOTE;
        } else if (qualifiers.contains(Qualifier.RESOURCE)) {
            return FunctionResult.Kind.RESOURCE;
        } else if (qualifiers.contains(Qualifier.PUBLIC)) {
            return FunctionResult.Kind.FUNCTION;
        }
        return FunctionResult.Kind.FUNCTION;
    }

    /**
     * Gets the description from a documentable element.
     *
     * @param documentable The documentable element
     * @return The description
     */
    private static String getDescription(Documentable documentable) {
        return documentable.documentation().flatMap(Documentation::description).orElse("");
    }

    /**
     * Gets the client type string.
     *
     * @param packageName The package name
     * @return The client type string
     */
    private static String getClientType(String packageName) {
        String importPrefix = packageName.substring(packageName.lastIndexOf('.') + 1);
        return String.format("%s:%s", importPrefix, "Client");
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
            } else {
                return expression.toSourceCode();
            }
        }
        return null;
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
}