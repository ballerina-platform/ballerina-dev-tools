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

package io.ballerina.modelgenerator.commons;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.Documentable;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
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
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory class to create FunctionResult instances from function symbols.
 *
 * @since 2.0.0
 */
public class FunctionResultBuilder {

    private SemanticModel semanticModel;
    private TypeSymbol errorTypeSymbol;
    private Package resolvedPackage;
    private FunctionSymbol functionSymbol;
    private FunctionResult.Kind functionResultKind;
    private String functionName;
    private String description;
    private String packageName;
    private ModuleInfo moduleInfo;
    private ModuleInfo userModuleInfo;

    public FunctionResultBuilder semanticModel(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
        this.errorTypeSymbol = semanticModel.types().ERROR;
        return this;
    }

    public FunctionResultBuilder resolvedPackage(Package resolvedPackage) {
        if (semanticModel == null) {
            semanticModel(resolvedPackage.getCompilation().getSemanticModel(
                    resolvedPackage.getDefaultModule().moduleId()));
        }
        this.resolvedPackage = resolvedPackage;
        return this;
    }

    public FunctionResultBuilder name(String name) {
        this.functionName = name;
        return this;
    }

    public FunctionResultBuilder functionDoc(Documentable documentable) {
        this.description = documentable.documentation().flatMap(Documentation::description).orElse("");
        return this;
    }

    public FunctionResultBuilder connectionDoc(Documentable documentable) {
        this.description = description;
        return this;
    }

    public FunctionResultBuilder packageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public FunctionResultBuilder moduleInfo(ModuleInfo moduleInfo) {
        this.moduleInfo = moduleInfo;
        return this;
    }

    public FunctionResultBuilder functionSymbol(FunctionSymbol functionSymbol) {
        if (moduleInfo == null) {
            functionSymbol.getModule().ifPresent(module -> moduleInfo = ModuleInfo.from(module.id()));
        }
        if (functionResultKind == null) {
            functionResultKind = FunctionResult.Kind.FUNCTION;
            if (functionSymbol.kind() == SymbolKind.METHOD) {
                List<Qualifier> qualifiers = functionSymbol.qualifiers();
                if (qualifiers.contains(Qualifier.REMOTE)) {
                    functionResultKind = FunctionResult.Kind.REMOTE;
                } else if (qualifiers.contains(Qualifier.RESOURCE)) {
                    functionResultKind = FunctionResult.Kind.RESOURCE;
                }
            }
        }
        this.functionSymbol = functionSymbol;
        return this;
    }

    public FunctionResultBuilder functionResultKind(FunctionResult.Kind kind) {
        this.functionResultKind = kind;
        return this;
    }

    public FunctionResultBuilder userModuleInfo(ModuleInfo moduleInfo) {
        this.userModuleInfo = moduleInfo;
        return this;
    }

    public FunctionResult build() {
        // The function name is required to build the FunctionResult
        if (this.functionName == null) {
            this.functionName = this.functionSymbol.getName()
                    .orElseThrow(() -> new IllegalStateException("Function name not found"));
        }

        // The module information is required to build the FunctionResult
        if (moduleInfo == null) {
            throw new IllegalStateException("Module information not found");
        }

        // Check if the function is in the index
        Optional<FunctionResult> indexedResult = getFunctionFromIndex();
        if (indexedResult.isPresent()) {
            return indexedResult.get();
        }

        // Fetch the semantic model if not provided
        if (semanticModel == null) {
            SemanticModel fetchedSemanticModel =
                    PackageUtil.getSemanticModel(moduleInfo.org(), moduleInfo.packageName(), moduleInfo.version())
                            .orElseThrow(() -> new IllegalStateException("Semantic model not found"));
            semanticModel(fetchedSemanticModel);
        }

        // Find the symbol if not provided
        if (functionSymbol == null) {
            FunctionSymbol fetchedSymbol = semanticModel.moduleSymbols().parallelStream()
                    .filter(moduleSymbol -> moduleSymbol.nameEquals(functionName) &&
                            moduleSymbol instanceof FunctionSymbol)
                    .map(moduleSymbol -> (FunctionSymbol) moduleSymbol)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Function symbol not found"));
            functionSymbol(fetchedSymbol);
        }

        if (description == null) {
            this.description = this.functionSymbol.documentation()
                    .flatMap(Documentation::description)
                    .orElse("");
        }

        // Obtain the return type of the function
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
        String returnType = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> getTypeSignature(returnTypeDesc))
                .orElse("");

        ParamForTypeInfer paramForTypeInfer = null;
        if (functionSymbol.external()) {
            List<String> paramNameList = new ArrayList<>();
            functionTypeSymbol.params().ifPresent(paramList -> paramList
                    .stream()
                    .filter(paramSym -> paramSym.paramKind() == ParameterKind.DEFAULTABLE)
                    .forEach(paramSymbol -> paramNameList.add(paramSymbol.getName().orElse(""))));

            Map<String, TypeSymbol> returnTypeMap =
                    allMembers(functionTypeSymbol.returnTypeDescriptor().orElse(null));
            for (String paramName : paramNameList) {
                if (returnTypeMap.containsKey(paramName)) {
                    returnType = "json";
                    String defaultValue =
                            DefaultValueGeneratorUtil.getDefaultValueForType(returnTypeMap.get(paramName));
                    paramForTypeInfer = new ParamForTypeInfer(paramName, defaultValue, returnType);
                    break;
                }
            }
        }

        boolean returnError = functionTypeSymbol.returnTypeDescriptor()
                .map(returnTypeDesc -> CommonUtils.subTypeOf(returnTypeDesc, errorTypeSymbol)).orElse(false);

        FunctionResult functionResult = new FunctionResult(0, functionName, description, returnType, packageName,
                moduleInfo.org(), moduleInfo.version(), null, FunctionResult.Kind.FUNCTION, returnError,
                paramForTypeInfer != null);

        Map<String, String> documentationMap =
                functionSymbol.documentation().map(Documentation::parameterMap).orElse(Map.of());
        ParamForTypeInfer finalParamForTypeInfer = paramForTypeInfer;
        Map<String, ParameterResult> parameters = new LinkedHashMap<>();
        functionTypeSymbol.params()
                .ifPresent(paramList -> paramList.forEach(
                        paramSymbol -> parameters.putAll(
                                getParameters(paramSymbol, documentationMap, finalParamForTypeInfer))));
        functionTypeSymbol.restParam()
                .ifPresent(paramSymbol -> parameters.putAll(
                        getParameters(paramSymbol, documentationMap, finalParamForTypeInfer)));
        functionResult.setParameters(parameters);
        return functionResult;
    }

    private Optional<FunctionResult> getFunctionFromIndex() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        Optional<FunctionResult> optFunctionResult =
                dbManager.getFunction(moduleInfo.org(), moduleInfo.packageName(), functionName,
                        DatabaseManager.FunctionKind.FUNCTION, null);
        if (optFunctionResult.isEmpty()) {
            return Optional.empty();
        }
        FunctionResult functionResult = optFunctionResult.get();
        LinkedHashMap<String, ParameterResult> parameters =
                dbManager.getFunctionParametersAsMap(functionResult.functionId());
        functionResult.setParameters(parameters);
        return Optional.of(functionResult);
    }

    private record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }

    private Map<String, ParameterResult> getParameters(ParameterSymbol paramSymbol,
                                                       Map<String, String> documentationMap,
                                                       ParamForTypeInfer paramForTypeInfer) {
        Map<String, ParameterResult> parameters = new LinkedHashMap<>();
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterResult.Kind parameterKind = ParameterResult.Kind.fromKind(paramSymbol.paramKind());
        String paramType;
        boolean optional = true;
        String defaultValue;
        TypeSymbol typeSymbol = paramSymbol.typeDescriptor();
        String importStatements = CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null);
        if (parameterKind == ParameterResult.Kind.REST_PARAMETER) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
            paramType = getTypeSignature(((ArrayTypeSymbol) typeSymbol).memberTypeDescriptor());
        } else if (parameterKind == ParameterResult.Kind.INCLUDED_RECORD) {
            paramType = getTypeSignature(typeSymbol);
            Map<String, ParameterResult> includedParameters =
                    getIncludedRecordParams((RecordTypeSymbol) CommonUtil.getRawType(typeSymbol), true,
                            new HashMap<>());
            parameters.putAll(includedParameters);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        } else if (parameterKind == ParameterResult.Kind.REQUIRED) {
            paramType = getTypeSignature(typeSymbol);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            optional = false;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    parameters.put(paramName, ParameterResult.from(paramName, paramDescription, paramType, defaultValue,
                            ParameterResult.Kind.PARAM_FOR_TYPE_INFER, optional, importStatements));
                    return parameters;
                }
            }
            defaultValue = getParamDefaultValue(paramSymbol, typeSymbol);
            paramType = getTypeSignature(typeSymbol);
        }
        parameters.put(paramName, ParameterResult.from(paramName, paramDescription, paramType, defaultValue,
                parameterKind, optional, importStatements));
        return parameters;
    }

    private Map<String, ParameterResult> getIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                                 boolean insert,
                                                                 Map<String, String> documentationMap) {
        Map<String, ParameterResult> parameters = new LinkedHashMap<>();
        recordTypeSymbol.typeInclusions().forEach(includedType -> parameters.putAll(
                getIncludedRecordParams((RecordTypeSymbol) includedType, insert, documentationMap))
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

            String defaultValue = getRecordFieldDefaultValue(recordFieldSymbol, fieldType);
            String paramType = getTypeSignature(typeSymbol);
            boolean optional = recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue();
            parameters.put(paramName, ParameterResult.from(paramName, documentationMap.get(paramName),
                    paramType, defaultValue, ParameterResult.Kind.INCLUDED_FIELD, optional,
                    CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null)));
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = getTypeSignature(typeSymbol);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            parameters.put("Additional Values", ParameterResult.from("Additional Values",
                    "Capture key value pairs", paramType, defaultValue,
                    ParameterResult.Kind.INCLUDED_RECORD_REST, true,
                    CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null)));
        });
        return parameters;
    }

    private String getParamDefaultValue(ParameterSymbol paramSymbol, TypeSymbol typeSymbol) {
        String defaultValue;
        Location symbolLocation = paramSymbol.getLocation().get();
        Document document = findDocument(resolvedPackage, symbolLocation.lineRange().fileName());
        defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
        if (document != null) {
            defaultValue = getParamDefaultValue(document.syntaxTree().rootNode(),
                    symbolLocation, resolvedPackage.packageName().value());
        }
        return defaultValue;
    }

    private String getRecordFieldDefaultValue(RecordFieldSymbol recordFieldSymbol, TypeSymbol fieldType) {
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
        return defaultValue;
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

    private Document findDocument(Package pkg, String path) {
        if (resolvedPackage == null) {
            return null;
        }
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

    private static Map<String, TypeSymbol> allMembers(TypeSymbol typeSymbol) {
        Map<String, TypeSymbol> members = new HashMap<>();
        if (typeSymbol == null) {
            return members;
        } else if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            unionTypeSymbol.memberTypeDescriptors()
                    .forEach(memberType -> members.put(memberType.getName().orElse(""), memberType));
        } else if (typeSymbol.typeKind() == TypeDescKind.INTERSECTION) {
            IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
            intersectionTypeSymbol.memberTypeDescriptors()
                    .forEach(memberType -> members.put(memberType.getName().orElse(""), memberType));
        } else {
            members.put(typeSymbol.getName().orElse(""), typeSymbol);
        }
        return members;
    }

    private String getTypeSignature(TypeSymbol typeSymbol) {
        if (userModuleInfo == null) {
            return CommonUtils.getTypeSignature(semanticModel, typeSymbol, false);
        }
        return CommonUtils.getTypeSignature(semanticModel, typeSymbol, false, userModuleInfo);
    }

}