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
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.modelgenerator.commons.CommonUtils;
import io.ballerina.modelgenerator.commons.DefaultValueGeneratorUtil;
import io.ballerina.modelgenerator.commons.ParameterData;

import java.util.ArrayList;
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

    public record ParamForTypeInfer(String paramName, String defaultValue, String type) {
    }
}
