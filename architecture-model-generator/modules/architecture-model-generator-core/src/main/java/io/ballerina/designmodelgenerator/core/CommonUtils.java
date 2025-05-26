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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathRestParam;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;

import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utility functions.
 *
 * @since 2.0.0
 */
public class CommonUtils {

    private static final String CENTRAL_ICON_URL = "https://bcentral-packageicons.azureedge.net/images/%s_%s_%s.png";
    private static final Pattern FULLY_QUALIFIED_MODULE_ID_PATTERN =
            Pattern.compile("(\\w+)/([\\w.]+):([^:]+):(\\w+)[|]?");
    private static final Random random = new Random();

    /**
     * Get the raw type of the type descriptor. If the type descriptor is a type reference then return the associated
     * type descriptor.
     *
     * @param typeDescriptor type descriptor to evaluate
     * @return {@link TypeSymbol} extracted type descriptor
     */
    public static TypeSymbol getRawType(TypeSymbol typeDescriptor) {
        if (typeDescriptor.typeKind() == TypeDescKind.INTERSECTION) {
            return getRawType(((IntersectionTypeSymbol) typeDescriptor).effectiveTypeDescriptor());
        }
        if (typeDescriptor.typeKind() == TypeDescKind.TYPE_REFERENCE) {
            TypeReferenceTypeSymbol typeRef = (TypeReferenceTypeSymbol) typeDescriptor;
            if (typeRef.typeDescriptor().typeKind() == TypeDescKind.INTERSECTION) {
                return getRawType(((IntersectionTypeSymbol) typeRef.typeDescriptor()).effectiveTypeDescriptor());
            }
            TypeSymbol rawType = typeRef.typeDescriptor();
            if (rawType.typeKind() == TypeDescKind.TYPE_REFERENCE) {
                return getRawType(rawType);
            }
            return rawType;
        }
        return typeDescriptor;
    }

    /**
     * Generates the URL for the icon in the Ballerina central.
     *
     * @param moduleID the module ID
     */
    public static String generateIcon(ModuleID moduleID) {
        return String.format(CENTRAL_ICON_URL, moduleID.orgName(), moduleID.packageName(), moduleID.version());
    }

    /**
     * Generates the icon URL for the given type symbol. If the module symbol is not present, the icon will not be
     * generated.
     *
     * @param typeSymbol the type symbol
     * @return the icon URL or null if the module symbol is not present
     */
    public static String generateIcon(TypeSymbol typeSymbol) {
        return typeSymbol.getModule().map(moduleSymbol -> generateIcon(moduleSymbol.id())).orElse(null);
    }

    public static String generateUUID() {
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    /**
     * Returns the processed type signature of the type symbol. It removes the organization and the package, and checks
     * if it is the default module which will remove the prefix.
     *
     * @param typeSymbol the type symbol
     * @param moduleInfo the default module name descriptor
     * @return the processed type signature
     */
    public static String getTypeSignature(TypeSymbol typeSymbol, ModuleInfo moduleInfo) {
        String text = typeSymbol.signature();
        StringBuilder newText = new StringBuilder();
        Matcher matcher = FULLY_QUALIFIED_MODULE_ID_PATTERN.matcher(text);
        int nextStart = 0;
        while (matcher.find()) {
            // Append up-to start of the match
            newText.append(text, nextStart, matcher.start(1));

            String modPart = matcher.group(2);
            int last = modPart.lastIndexOf(".");
            if (last != -1) {
                modPart = modPart.substring(last + 1);
            }

            String typeName = matcher.group(4);

            if (!modPart.equals(moduleInfo.packageName())) {
                newText.append(modPart);
                newText.append(":");
            }
            newText.append(typeName);
            // Update next-start position
            nextStart = matcher.end(4);
        }
        // Append the remaining
        if (nextStart != 0 && nextStart < text.length()) {
            newText.append(text.substring(nextStart));
        }
        return !newText.isEmpty() ? newText.toString() : text;
    }

    public record ModuleInfo(String org, String packageName, String moduleName, String version) {

        public static ModuleInfo from(ModuleID moduleId) {
            return new ModuleInfo(moduleId.orgName(), moduleId.packageName(), moduleId.moduleName(),
                    moduleId.version());
        }
    }

    /**
     * Returns the resource path string for the given resource method symbol.
     *
     * @param semanticModel the semantic model
     * @param resourceMethodSymbol the resource method symbol
     *
     * @return the resource path string
     */
    public static String getResourcePathStr(SemanticModel semanticModel,
                                            ResourceMethodSymbol resourceMethodSymbol) {

        io.ballerina.modelgenerator.commons.ModuleInfo moduleInfo;
        if (resourceMethodSymbol.getName().isPresent()) {
            moduleInfo = io.ballerina.modelgenerator.commons.ModuleInfo.from(
                    resourceMethodSymbol.getModule().get().id());
        } else {
            moduleInfo = null;
        }

        StringBuilder pathBuilder = new StringBuilder();
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                boolean isFirstElement = true;
                for (Symbol pathSegment : pathSegmentList.list()) {
                    if (isFirstElement) {
                        isFirstElement = false;
                    } else {
                        pathBuilder.append("/");
                    }
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        String type = io.ballerina.modelgenerator.commons.CommonUtils.getTypeSignature(semanticModel,
                                pathParameterSymbol.typeDescriptor(), true, moduleInfo);
                        pathBuilder.append("[").append(type);
                        String paramName = pathParameterSymbol.getName().orElse("");
                        if (!paramName.isEmpty()) {
                            pathBuilder.append(" ").append(paramName);
                        }
                        pathBuilder.append("]");
                    } else {
                        pathBuilder.append(pathSegment.getName().orElse(""));
                    }
                }
                ((PathSegmentList) resourcePath).pathRestParameter().ifPresent(pathRestParameter -> {
                    String type = io.ballerina.modelgenerator.commons.CommonUtils.getTypeSignature(semanticModel,
                            pathRestParameter.typeDescriptor(), true, moduleInfo);
                    pathBuilder.append("[").append(type).append("...");
                    if (!pathRestParameter.isTypeOnlyParam()) {
                        pathBuilder.append(" ").append(pathRestParameter.getName().orElse(""));
                    }
                    pathBuilder.append("]");
                });
            }
            case PATH_REST_PARAM -> {
                PathParameterSymbol pathRestParameter = ((PathRestParam) resourcePath).parameter();
                String type = io.ballerina.modelgenerator.commons.CommonUtils.getTypeSignature(semanticModel,
                        pathRestParameter.typeDescriptor(), true, moduleInfo);
                pathBuilder.append("[").append(type).append("...");
                if (!pathRestParameter.isTypeOnlyParam()) {
                    pathBuilder.append(" ").append(pathRestParameter.getName().orElse(""));
                }
                pathBuilder.append("]");
            }
            case DOT_RESOURCE_PATH -> pathBuilder.append(".");
        }
        return pathBuilder.toString();
    }
}
