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

package io.ballerina.flowmodelgenerator.core;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.PathParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordFieldSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.ResourceMethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.api.symbols.resourcepath.PathRestParam;
import io.ballerina.compiler.api.symbols.resourcepath.PathSegmentList;
import io.ballerina.compiler.api.symbols.resourcepath.ResourcePath;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ChildNodeList;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.utils.DefaultValueGeneratorUtil;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Common utility functions used in the project.
 *
 * @since 1.4.0
 */
public class CommonUtils {

    private static final String CENTRAL_ICON_URL = "https://bcentral-packageicons.azureedge.net/images/%s_%s_%s.png";
    private static final Pattern FULLY_QUALIFIED_MODULE_ID_PATTERN =
            Pattern.compile("(\\w+)/([\\w.]+):([^:]+):(\\w+)[|]?");

    /**
     * Removes the quotes from the given string.
     *
     * @param inputString the input string
     * @return the string without quotes
     */
    public static String removeQuotes(String inputString) {
        return inputString.replaceAll("^\"|\"$", "");
    }

    public static String getProjectName(Document document) {
        return document.module().descriptor().packageName().value();
    }

    /**
     * Retrieves the type signature of the given type symbol.
     *
     * @param semanticModel    the semantic model
     * @param typeSymbol       the type symbol
     * @param ignoreError      whether to ignore errors
     * @param moduleDescriptor the default module descriptor
     * @return the type signature
     * @see #getTypeSignature(TypeSymbol, ModuleDescriptor)
     */
    public static String getTypeSignature(SemanticModel semanticModel, TypeSymbol typeSymbol, boolean ignoreError,
                                          ModuleDescriptor moduleDescriptor) {
        if (typeSymbol.typeKind() == TypeDescKind.UNION) {
            UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
            return unionTypeSymbol.memberTypeDescriptors().stream()
                    .filter(memberType -> !ignoreError || !memberType.subtypeOf(semanticModel.types().ERROR))
                    .map(type -> getTypeSignature(semanticModel, type, ignoreError, moduleDescriptor))
                    .reduce((s1, s2) -> s1 + "|" + s2)
                    .orElse(getTypeSignature(unionTypeSymbol, moduleDescriptor));
        }
        return getTypeSignature(typeSymbol, moduleDescriptor);
    }

    /**
     * Retrieves the type signature of the given type symbol.
     *
     * @param semanticModel the semantic model
     * @param typeSymbol    the type symbol
     * @param ignoreError   whether to ignore errors
     * @return the type signature
     * @see #getTypeSignature(TypeSymbol, ModuleDescriptor)
     */
    public static String getTypeSignature(SemanticModel semanticModel, TypeSymbol typeSymbol, boolean ignoreError) {
        return getTypeSignature(semanticModel, typeSymbol, ignoreError, null);
    }

    /**
     * Returns the processed type signature of the type symbol. It removes the organization and the package, and checks
     * if it is the default module which will remove the prefix.
     *
     * @param typeSymbol       the type symbol
     * @param moduleDescriptor the default module name descriptor
     * @return the processed type signature
     */
    public static String getTypeSignature(TypeSymbol typeSymbol, ModuleDescriptor moduleDescriptor) {
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

            if (!modPart.equals(moduleDescriptor.packageName().value())) {
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

    /**
     * Returns the module name of the given symbol.
     *
     * @param symbol the symbol to get the module name
     * @return the module name
     */
    public static String getModuleName(Symbol symbol) {
        return symbol.getModule().flatMap(Symbol::getName).orElse("");
    }

    /**
     * Returns the organization name of the given symbol.
     *
     * @param symbol the symbol to get the organization name
     * @return the organization name
     */
    public static String getOrgName(Symbol symbol) {
        return symbol.getModule()
                .map(module -> module.id().orgName())
                .orElse("");
    }

    /**
     * Returns the expression node with check expression if exists.
     *
     * @param expressionNode the expression node
     * @return the expression node with check expression if exists
     */
    public static NonTerminalNode getExpressionWithCheck(NonTerminalNode expressionNode) {
        NonTerminalNode parentNode = expressionNode.parent();
        return parentNode.kind() == SyntaxKind.CHECK_EXPRESSION ? parentNode : expressionNode;
    }

    /**
     * Returns the node in the syntax tree for the given text range.
     *
     * @param syntaxTree the syntax tree in which the node resides
     * @param textRange  the text range of the node
     * @return the node in the syntax tree
     */
    public static NonTerminalNode getNode(SyntaxTree syntaxTree, TextRange textRange) {
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        return modulePartNode.findNode(textRange, true);
    }

    /**
     * Convert the syntax-node line range into a lsp4j range.
     *
     * @param lineRange line range
     * @return {@link Range} converted range
     */
    public static Range toRange(LineRange lineRange) {
        return new Range(toPosition(lineRange.startLine()), toPosition(lineRange.endLine()));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param position line position
     * @return {@link Range} converted range
     */
    public static Range toRange(LinePosition position) {
        return new Range(toPosition(position), toPosition(position));
    }

    /**
     * Converts syntax-node line position into a lsp4j position.
     *
     * @param linePosition - line position
     * @return {@link Position} converted position
     */
    public static Position toPosition(LinePosition linePosition) {
        return new Position(linePosition.line(), linePosition.offset());
    }

    /**
     * Get the type symbol of the given node.
     *
     * @param semanticModel the semantic model
     * @param node          the node to get the type symbol
     * @return the type symbol
     */
    public static Optional<TypeSymbol> getTypeSymbol(SemanticModel semanticModel, Node node) {
        if (node.kind() == SyntaxKind.TYPED_BINDING_PATTERN) {
            TypedBindingPatternNode typedBindingPatternNode = (TypedBindingPatternNode) node;
            BindingPatternNode bindingPatternNode = typedBindingPatternNode.bindingPattern();

            Optional<Symbol> typeDescriptorSymbol = semanticModel.symbol(typedBindingPatternNode.typeDescriptor());
            if (typeDescriptorSymbol.isPresent() && typeDescriptorSymbol.get().kind() == SymbolKind.TYPE) {
                return Optional.of((TypeSymbol) typeDescriptorSymbol.get());
            }

            Optional<Symbol> bindingPatternSymbol = semanticModel.symbol(bindingPatternNode);
            if (bindingPatternSymbol.isPresent() && bindingPatternSymbol.get().kind() == SymbolKind.VARIABLE) {
                return Optional.ofNullable(((VariableSymbol) bindingPatternSymbol.get()).typeDescriptor());
            }
        }
        return semanticModel.typeOf(node);
    }

    /**
     * Get the variable name from the given node.
     *
     * @param node the node to get the variable name
     * @return the variable name
     */
    public static String getVariableName(Node node) {
        if (node.kind() == SyntaxKind.TYPED_BINDING_PATTERN) {
            return ((TypedBindingPatternNode) node).bindingPattern().toString().strip();
        }
        if (node instanceof BuiltinSimpleNameReferenceNode builtinSimpleNameReferenceNode) {
            return builtinSimpleNameReferenceNode.name().text();
        }
        if (node instanceof SimpleNameReferenceNode simpleNameReferenceNode) {
            return simpleNameReferenceNode.name().text();
        }
        return node.toString().strip();
    }

    /**
     * Returns the default value for the given API doc type.
     *
     * @param type the type to get the default value for
     * @return the default value for the given type
     */
    public static String getDefaultValueForType(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "inclusion", "record" -> "{}";
            case "string" -> "\"\"";
            default -> "";
        };
    }

    /**
     * Checks if the query map has no keyword.
     *
     * @param queryMap the query map to check
     * @return true if the query map has no keyword, false otherwise
     */
    public static boolean hasNoKeyword(Map<String, String> queryMap, String keyName) {
        return queryMap == null || queryMap.isEmpty() || !queryMap.containsKey(keyName) ||
                queryMap.get(keyName).isEmpty();
    }

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
     * Retrieves the document from the given project and location.
     *
     * @param project  the project to retrieve the document from
     * @param location the location of the document
     * @return the document at the specified location
     */
    public static Document getDocument(Project project, Location location) {
        DocumentId documentId = project.documentId(
                project.kind() == ProjectKind.SINGLE_FILE_PROJECT ? project.sourceRoot() :
                        project.sourceRoot().resolve(location.lineRange().fileName()));
        return project.currentPackage().getDefaultModule().document(documentId);
    }

    /***
     * Check whether the given line range is within a do clause.
     *
     * @param workspaceManager the workspace manager
     * @param filePath the file path
     * @param lineRange the line range
     * @return true if the line range is within a do clause, false otherwise
     */
    public static boolean withinDoClause(WorkspaceManager workspaceManager, Path filePath, LineRange lineRange) {
        try {
            workspaceManager.loadProject(filePath);
            Document document = workspaceManager.document(filePath).orElseThrow();
            int startPos = document.textDocument().textPositionFrom(lineRange.startLine());
            int endPos = document.textDocument().textPositionFrom(lineRange.endLine());
            ModulePartNode node = document.syntaxTree().rootNode();
            NonTerminalNode currentNode = node.findNode(TextRange.from(startPos, endPos - startPos),
                    true);
            while (currentNode != null) {
                if (currentNode.kind() == SyntaxKind.DO_STATEMENT) {
                    return ((DoStatementNode) currentNode).onFailClause().isPresent();
                }
                currentNode = currentNode.parent();
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Check whether the given node is within a do clause.
     *
     * @param node the node to check
     * @return true if the node is within a do clause, false otherwise
     */
    public static boolean withinDoClause(NonTerminalNode node) {
        while (node != null) {
            if (node.kind() == SyntaxKind.DO_STATEMENT) {
                return ((DoStatementNode) node).onFailClause().isPresent();
            }
            node = node.parent();
        }
        return false;
    }

    /**
     * Generates the URL for the icon in the Ballerina central.
     *
     * @param orgName     the organization name
     * @param packageName the package name
     * @param versionName the version name
     * @return the URL for the icon
     */
    public static String generateIcon(String orgName, String packageName, String versionName) {
        return String.format(CENTRAL_ICON_URL, orgName, packageName, versionName);
    }

    /**
     * Builds the resource path template for the given function symbol.
     *
     * @param functionSymbol the function symbol
     * @return the resource path template
     */
    public static String buildResourcePathTemplate(FunctionSymbol functionSymbol) {
        StringBuilder pathBuilder = new StringBuilder();
        ResourceMethodSymbol resourceMethodSymbol = (ResourceMethodSymbol) functionSymbol;
        ResourcePath resourcePath = resourceMethodSymbol.resourcePath();
        switch (resourcePath.kind()) {
            case PATH_SEGMENT_LIST -> {
                PathSegmentList pathSegmentList = (PathSegmentList) resourcePath;
                for (Symbol pathSegment : pathSegmentList.list()) {
                    pathBuilder.append("/");
                    if (pathSegment instanceof PathParameterSymbol pathParameterSymbol) {
                        String value = DefaultValueGeneratorUtil
                                .getDefaultValueForType(pathParameterSymbol.typeDescriptor());
                        pathBuilder.append("[").append(value).append("]");
                    } else {
                        pathBuilder.append(pathSegment.getName().orElse(""));
                    }
                }
                ((PathSegmentList) resourcePath).pathRestParameter().ifPresent(pathRestParameter -> {
                    String type = CommonUtil.getRawType(pathRestParameter.typeDescriptor())
                            .signature();
                    pathBuilder.append("[").append(type).append("...]");
                });
            }
            case PATH_REST_PARAM -> pathBuilder.append("[").append("/path/to/resource").append("]");
            case DOT_RESOURCE_PATH -> pathBuilder.append("\\.");
        }
        return pathBuilder.toString();
    }

    /**
     * Check whether the given type is a subtype of the target type.
     *
     * @param source the source type
     * @param target the target type
     * @return true if the source type is a subtype of the target type, false otherwise
     */
    public static boolean subTypeOf(TypeSymbol source, TypeSymbol target) {
        TypeSymbol sourceRawType = CommonUtils.getRawType(source);
        switch (sourceRawType.typeKind()) {
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) sourceRawType;
                return unionTypeSymbol.memberTypeDescriptors().stream().anyMatch(type -> subTypeOf(type, target));
            }
            case TYPEDESC -> {

            }
            case INTERSECTION -> {
                IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) sourceRawType;
                return intersectionTypeSymbol.memberTypeDescriptors().stream()
                        .anyMatch(t -> subTypeOf(t, target));
            }
            case TYPE_REFERENCE -> {
                return subTypeOf(sourceRawType, target);
            }
            default -> {
                return sourceRawType.subtypeOf(target);
            }
        }
        return sourceRawType.subtypeOf(target);
    }

    public static LinkedHashMap<String, ParameterResult> buildFunctionParamResultMap(FunctionSymbol functionSymbol, SemanticModel semanticModel) {
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
        LinkedHashMap<String, ParameterResult> funcParamMap = new LinkedHashMap<>();
        functionTypeSymbol.params().ifPresent(paramList -> paramList.forEach(paramSymbol ->
                buildFunctionParamMap(paramSymbol, documentationMap, semanticModel, funcParamMap,
                        finalParamForTypeInfer)));
        functionTypeSymbol.restParam().ifPresent(paramSymbol ->
                buildFunctionParamMap(paramSymbol, documentationMap, semanticModel, funcParamMap, null));
        return funcParamMap;
    }

    private static void buildFunctionParamMap(ParameterSymbol paramSymbol,
                                                        Map<String, String> documentationMap,
                                                        SemanticModel semanticModel,
                                                         LinkedHashMap<String, ParameterResult> funcParamMap,
                                                        ParamForTypeInfer paramForTypeInfer) {
        String paramName = paramSymbol.getName().orElse("");
        String paramDescription = documentationMap.get(paramName);
        ParameterKind parameterKind = paramSymbol.paramKind();
        String paramType;
        int optional = 1;
        String defaultValue;
        Parameter.Kind kind;
        String moduleName = paramSymbol.getModule().get().id().moduleName();
        if (parameterKind == ParameterKind.REST) {
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(
                    ((ArrayTypeSymbol) paramSymbol.typeDescriptor()).memberTypeDescriptor());
            paramType = getTypeSignature(semanticModel,
                    ((ArrayTypeSymbol) paramSymbol.typeDescriptor()).memberTypeDescriptor(),
                    true, moduleName);
            kind = Parameter.Kind.REST;
        } else if (parameterKind == ParameterKind.INCLUDED_RECORD) {
            paramType = getTypeSignature(semanticModel, paramSymbol.typeDescriptor(), true, moduleName);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            kind = Parameter.Kind.INCLUDED_RECORD;
            buildIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(paramSymbol.typeDescriptor()),
                    semanticModel, moduleName, funcParamMap);
        } else if (parameterKind == ParameterKind.REQUIRED) {
            paramType = getTypeSignature(semanticModel, paramSymbol.typeDescriptor(), true, moduleName);
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            optional = 0;
            kind = Parameter.Kind.REQUIRED;
        } else {
            if (paramForTypeInfer != null) {
                if (paramForTypeInfer.paramName().equals(paramName)) {
                    defaultValue = paramForTypeInfer.type();
                    paramType = paramForTypeInfer.type();
                    funcParamMap.put(paramName, new ParameterResult(0, paramName, paramType,
                            Parameter.Kind.PARAM_FOR_TYPE_INFER, defaultValue, paramDescription, 1));
                    return;
                }
            }
            defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(paramSymbol.typeDescriptor());
            paramType = getTypeSignature(semanticModel, paramSymbol.typeDescriptor(), true, moduleName);
            kind = Parameter.Kind.DEFAULTABLE;
        }
        funcParamMap.put(paramName, new ParameterResult(0, paramName, paramType, kind,
                defaultValue, paramDescription, optional));
    }

    private static void buildIncludedRecordParams(RecordTypeSymbol recordTypeSymbol,
                                                      SemanticModel semanticModel, String moduleName,
                                                      LinkedHashMap<String, ParameterResult> funcParamMap) {
        recordTypeSymbol.typeInclusions().forEach(includedType ->
            buildIncludedRecordParams((RecordTypeSymbol) CommonUtils.getRawType(includedType),
                    semanticModel, moduleName, funcParamMap));

        for (Map.Entry<String, RecordFieldSymbol> entry : recordTypeSymbol.fieldDescriptors().entrySet()) {
            RecordFieldSymbol recordFieldSymbol = entry.getValue();
            TypeSymbol fieldType = CommonUtil.getRawType(recordFieldSymbol.typeDescriptor());
            if (fieldType.typeKind() == TypeDescKind.NEVER) {
                continue;
            }
            String paramName = entry.getKey();
            String defaultValue= DefaultValueGeneratorUtil.getDefaultValueForType(fieldType);
            String paramDescription = entry.getValue().documentation()
                    .flatMap(Documentation::description).orElse("");
            String paramType = getTypeSignature(semanticModel, fieldType, true, moduleName);
            int optional = 0;
            if (recordFieldSymbol.isOptional() || recordFieldSymbol.hasDefaultValue()) {
                optional = 1;
            }
            funcParamMap.put(paramName, new ParameterResult(0, paramName, paramType,
                    Parameter.Kind.INCLUDED_RECORD_ATTRIBUTE, defaultValue, paramDescription, optional));
        }
        recordTypeSymbol.restTypeDescriptor().ifPresent(typeSymbol -> {
            String paramType =  getTypeSignature(semanticModel, typeSymbol, true, moduleName);
            String defaultValue = DefaultValueGeneratorUtil.getDefaultValueForType(typeSymbol);
            funcParamMap.put(Parameter.Kind.INCLUDED_RECORD_REST.name(), new ParameterResult(0,
                    Parameter.Kind.INCLUDED_RECORD_REST.name(), paramType,
                    Parameter.Kind.INCLUDED_RECORD_REST, defaultValue, "Key value pair for", 1));
        });
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

    public record ParamForTypeInfer(String paramName, String defaultValue, String type) {}

    //TODO: Remove this once the diagnostic helper is exposed to LS extensions
    public static Diagnostic transformBallerinaDiagnostic(io.ballerina.tools.diagnostics.Diagnostic diag) {
        LineRange lineRange = diag.location().lineRange();
        int startLine = lineRange.startLine().line();
        int startChar = lineRange.startLine().offset();
        int endLine = lineRange.endLine().line();
        int endChar = lineRange.endLine().offset();

        endLine = (endLine <= 0) ? startLine : endLine;
        endChar = (endChar <= 0) ? startChar + 1 : endChar;

        Range range = new Range(new Position(startLine, startChar), new Position(endLine, endChar));
        Diagnostic diagnostic = new Diagnostic(range, diag.message(), null, null, diag.diagnosticInfo().code());

        switch (diag.diagnosticInfo().severity()) {
            case ERROR:
                diagnostic.setSeverity(DiagnosticSeverity.Error);
                break;
            case WARNING:
                diagnostic.setSeverity(DiagnosticSeverity.Warning);
                break;
            case HINT:
                diagnostic.setSeverity(DiagnosticSeverity.Hint);
                break;
            case INFO:
                diagnostic.setSeverity(DiagnosticSeverity.Information);
                break;
            default:
                break;
        }
        return diagnostic;
    }

    /**
     * Get the line range of a block node, excluding the opening and closing braces if they exist.
     *
     * @param node the block node
     * @return the line range of the block node
     */
    public static LineRange getLineRangeOfBlockNode(NonTerminalNode node) {
        ChildNodeList children = node.children();
        int size = children.size();

        if (size < 2) {
            return node.lineRange();
        }

        Node startToken = children.get(0);
        Node endToken = children.get(size - 1);

        if (startToken.kind() == SyntaxKind.OPEN_BRACE_TOKEN && endToken.kind() == SyntaxKind.CLOSE_BRACE_TOKEN) {
            return LineRange.from(node.lineRange().fileName(), startToken.lineRange().endLine(),
                    endToken.lineRange().startLine());
        }
        return node.lineRange();
    }

    /**
     * Checks if the given symbol belongs to the default package.
     *
     * @param symbol           the symbol to check
     * @param moduleDescriptor the module descriptor of the current module
     * @return true if the symbol belongs to the default package, false otherwise
     * @see #isDefaultPackage(String, String, ModuleDescriptor)
     */
    public static boolean isDefaultPackage(Symbol symbol, ModuleDescriptor moduleDescriptor) {
        Optional<ModuleID> moduleId = symbol.getModule().map(ModuleSymbol::id);
        return moduleId.filter(
                moduleID -> isDefaultPackage(moduleID.orgName(), moduleID.moduleName(), moduleDescriptor)).isPresent();
    }

    /**
     * Checks if the given module is the default package.
     *
     * @param orgName          the organization name
     * @param packageName      the package name
     * @param moduleDescriptor the module descriptor of the current module
     * @return true if the module is the default package, false otherwise
     */
    public static boolean isDefaultPackage(String orgName, String packageName, ModuleDescriptor moduleDescriptor) {
        return (orgName.equals(moduleDescriptor.org().value()) &&
                packageName.equals(moduleDescriptor.packageName().value()));
    }
}
