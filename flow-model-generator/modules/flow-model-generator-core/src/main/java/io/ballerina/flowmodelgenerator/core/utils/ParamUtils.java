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

package io.ballerina.flowmodelgenerator.core.utils;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DefaultValueGeneratorUtil;
import io.ballerina.modelgenerator.commons.ModuleInfo;
import io.ballerina.modelgenerator.commons.ParameterData;
import org.ballerinalang.langserver.common.utils.CommonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for handling parameters of functions and methods.
 *
 * @since 2.0.0
 */
public class ParamUtils {

    public static final String REST_RESOURCE_PATH = "/path/to/subdirectory";
    public static final String REST_PARAM_PATH = "/path/to/resource";
    public static final String REST_RESOURCE_PATH_LABEL = "Remaining Resource Path";

    /**
     * Builds the resource path template for the given function symbol.
     *
     * @param functionSymbol the function symbol
     * @return the resource path template
     */
    public static ResourcePathTemplate buildResourcePathTemplate(SemanticModel semanticModel,
                                                                 FunctionSymbol functionSymbol,
                                                                 TypeSymbol errorTypeSymbol) {
        Map<String, String> documentationMap = functionSymbol.documentation().map(Documentation::parameterMap)
                .orElse(Map.of());
        StringBuilder pathBuilder = new StringBuilder();
        ResourceMethodSymbol resourceMethodSymbol = (ResourceMethodSymbol) functionSymbol;
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        List<ParameterData> pathParams = new ArrayList<>();
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                for (Symbol pathSegment : pathSegmentList.list()) {
                    pathBuilder.append("/");
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        String defaultValue = DefaultValueGeneratorUtil
                                .getDefaultValueForType(pathParameterSymbol.typeDescriptor());
                        String type = CommonUtils.getTypeSignature(semanticModel, pathParameterSymbol.typeDescriptor(),
                                true);
                        String paramName = pathParameterSymbol.getName().orElse("");
                        String paramDescription = documentationMap.get(paramName);
                        pathBuilder.append("[").append(paramName).append("]");
                        pathParams.add(ParameterData.from(paramName, type, ParameterData.Kind.PATH_PARAM, defaultValue,
                                paramDescription, false));
                    } else {
                        pathBuilder.append(pathSegment.getName().orElse(""));
                    }
                }
                ((PathSegmentList) resourcePath).pathRestParameter().ifPresent(pathRestParameter -> {
                    pathParams.add(ParameterData.from(REST_RESOURCE_PATH_LABEL, "string",
                            ParameterData.Kind.PATH_REST_PARAM, REST_PARAM_PATH, REST_RESOURCE_PATH_LABEL,
                            false));
                });
            }
            case PATH_REST_PARAM -> {
                pathBuilder.append(REST_RESOURCE_PATH);
            }
            case DOT_RESOURCE_PATH -> pathBuilder.append("/");
        }
        return new ResourcePathTemplate(pathBuilder.toString(), pathParams);
    }

    public record ResourcePathTemplate(String resourcePathTemplate, List<ParameterData> pathParams) {
    }

    /**
     * Removes the leading single quote from the input string if it exists.
     *
     * @param input the input string
     * @return the modified string with the leading single quote removed
     */
    public static String removeLeadingSingleQuote(String input) {
        if (input != null && input.startsWith("'")) {
            return input.substring(1);
        }
        return input;
    }

    public static LinkedHashMap<String, ParameterData> buildFunctionParamResultMap(FunctionSymbol functionSymbol,
                                                                                   SemanticModel semanticModel) {
        ParamForTypeInfer paramForTypeInfer = null;
        FunctionTypeSymbol functionTypeSymbol = functionSymbol.typeDescriptor();
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
                    String defaultValue = DefaultValueGeneratorUtil
                            .getDefaultValueForType(returnTypeMap.get(paramName));
                    paramForTypeInfer = new ParamForTypeInfer(paramName, defaultValue, "json");
                    break;
                }
            }
        }
        final ParamForTypeInfer finalParamForTypeInfer = paramForTypeInfer;
        Map<String, String> documentationMap =
                functionSymbol.documentation().map(Documentation::parameterMap).orElse(Map.of());
        LinkedHashMap<String, ParameterData> funcParamMap = new LinkedHashMap<>();
        functionTypeSymbol.params().ifPresent(paramList -> paramList.forEach(paramSymbol ->
                buildFunctionParamMap(paramSymbol, documentationMap, semanticModel, funcParamMap,
                        finalParamForTypeInfer)));
        functionTypeSymbol.restParam().ifPresent(paramSymbol ->
                buildFunctionParamMap(paramSymbol, documentationMap, semanticModel, funcParamMap, null));
        return funcParamMap;
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

    private static void buildIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                  SemanticModel semanticModel, ModuleInfo moduleInfo,
                                                  LinkedHashMap<String, ParameterData> funcParamMap) {
        recordTypeSymbol.typeInclusions().forEach(includedType ->
                buildIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(includedType),
                        semanticModel, moduleInfo, funcParamMap));

        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol recordFieldTypeDescriptor = recordFieldSymbol.typeDescriptor();
            TypeSymbol fieldType = CommonUtil.getRawType(recordFieldTypeDescriptor);
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
            String paramDescription = entry.getValue().documentation()
                    .flatMap(Documentation::description).orElse("");
            String paramType = CommonUtils.getTypeSignature(semanticModel, recordFieldTypeDescriptor, true, moduleInfo);
            boolean optional = recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue();
            funcParamMap.put(paramName, new ParameterData(0, paramName, paramType,
                    ParameterData.Kind.INCLUDED_FIELD, defaultValue, paramDescription, optional,
                    CommonUtils.getImportStatements(recordFieldTypeDescriptor, moduleInfo).orElse(null)));
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType = CommonUtils.getTypeSignature(semanticModel, typeSymbol, true, moduleInfo);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            funcParamMap.put(ParameterData.Kind.INCLUDED_RECORD_REST.name(), new ParameterData(0,
                    "Additional Values", paramType,
                    ParameterData.Kind.INCLUDED_RECORD_REST, defaultValue, "Capture key value pairs", true,
                    CommonUtils.getImportStatements(typeSymbol, moduleInfo).orElse(null)));
        });
    }

    private static void buildFunctionParamMap(ParameterSymbol paramSymbol,
                                              Map<String, String> documentationMap,
                                              SemanticModel semanticModel,
                                              LinkedHashMap<String, ParameterData> funcParamMap,
                                              ParamForTypeInfer paramForTypeInfer) {
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterKind parameterKind = paramSymbol.paramKind();
        String paramType;
        boolean optional = true;
        String defaultValue;
        ParameterData.Kind kind;
        ModuleInfo moduleInfo = ModuleInfo.from(paramSymbol.getModule().get().id());
        TypeSymbol paramTypeDescriptor = paramSymbol.typeDescriptor();
        String importStatements = CommonUtils.getImportStatements(
                paramTypeDescriptor, moduleInfo).orElse(null);
        if (parameterKind == ParameterKind.REST) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) paramTypeDescriptor).memberTypeDescriptor());
            paramType = CommonUtils.getTypeSignature(semanticModel,
                    ((ArrayTypeSymbol) paramTypeDescriptor).memberTypeDescriptor(),
                    true, moduleInfo);
            kind = ParameterData.Kind.REST_PARAMETER;
        } else if (parameterKind == ParameterKind.INCLUDED_RECORD) {
            paramType = CommonUtils.getTypeSignature(semanticModel, paramTypeDescriptor, true,
                    moduleInfo);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramTypeDescriptor);
            kind = ParameterData.Kind.INCLUDED_RECORD;
            buildIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(paramTypeDescriptor),
                    semanticModel, moduleInfo, funcParamMap);
        } else if (parameterKind == ParameterKind.REQUIRED) {
            paramType = CommonUtils.getTypeSignature(semanticModel, paramTypeDescriptor, true, moduleInfo);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramTypeDescriptor);
            optional = false;
            kind = ParameterData.Kind.REQUIRED;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    funcParamMap.put(paramName,
                            new ParameterData(0, paramName, paramType, ParameterData.Kind.PARAM_FOR_TYPE_INFER,
                                    defaultValue, paramDescription, true, importStatements));
                    return;
                }
            }
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramTypeDescriptor);
            paramType = CommonUtils.getTypeSignature(semanticModel, paramTypeDescriptor, true,
                    moduleInfo);
            kind = ParameterData.Kind.DEFAULTABLE;
        }
        funcParamMap.put(paramName, new ParameterData(0, paramName, paramType, kind,
                defaultValue, paramDescription, optional, importStatements));
    }

    public record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }
}
