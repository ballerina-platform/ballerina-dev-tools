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

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.FutureTypeSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.MapTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.StreamTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TableTypeSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.BindingPatternNode;
import io.ballerina.compiler.syntax.tree.BuiltinSimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.ChildNodeList;
import io.ballerina.compiler.syntax.tree.DoStatementNode;
import io.ballerina.compiler.syntax.tree.ExpressionFunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.ModuleDescriptor;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.langserver.common.utils.CommonUtil;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Common utility functions used in the project.
 *
 * @since 2.0.0
 */
public class CommonUtils {

    private static final String CENTRAL_ICON_URL = "https://bcentral-packageicons.azureedge.net/images/%s_%s_%s.png";
    private static final Pattern FULLY_QUALIFIED_MODULE_ID_PATTERN =
            Pattern.compile("(\\w+)/([\\w.]+):([^:]+):(\\w+)[|]?");
    public static final String BALLERINA_ORG_NAME = "ballerina";
    public static final String BALLERINAX_ORG_NAME = "ballerinax";
    public static final String LANG_LIB_PREFIX = "lang.";
    private static final String NATURAL_FUNCTION = "NaturalFunction";
    private static final String CALL_LLM = "callLlm";
    private static final String UNKNOWN_TYPE = "Unknown Type";

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
     * @param semanticModel the semantic model
     * @param typeSymbol    the type symbol
     * @param ignoreError   whether to ignore errors
     * @param moduleInfo    the default module descriptor
     * @return the type signature
     * @see #getTypeSignature(TypeSymbol, ModuleInfo)
     */
    public static String getTypeSignature(SemanticModel semanticModel, TypeSymbol typeSymbol, boolean ignoreError,
                                          ModuleInfo moduleInfo) {
        return switch (typeSymbol.typeKind()) {
            case COMPILATION_ERROR -> UNKNOWN_TYPE;
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                yield unionTypeSymbol.memberTypeDescriptors().stream()
                        .filter(memberType -> !ignoreError || !memberType.subtypeOf(semanticModel.types().ERROR))
                        .map(type -> getTypeSignature(semanticModel, type, ignoreError, moduleInfo))
                        .reduce((s1, s2) -> s1 + "|" + s2)
                        .orElse(getTypeSignature(unionTypeSymbol, moduleInfo));
            }
            // TODO: This only address how type descriptors work with dependent types. Need to extend this to improve
            //  on handling cases where functions take type descriptors as parameters.
            case TYPEDESC -> {
                TypeDescTypeSymbol typeDescTypeSymbol = (TypeDescTypeSymbol) typeSymbol;
                yield typeDescTypeSymbol.typeParameter()
                        .map(typeParameter -> getTypeSignature(semanticModel, typeParameter, ignoreError, null))
                        .orElse(getTypeSignature(typeDescTypeSymbol, moduleInfo));
            }
            default -> getTypeSignature(typeSymbol, moduleInfo);
        };
    }

    /**
     * Retrieves the type signature of the given type symbol.
     *
     * @param semanticModel the semantic model
     * @param typeSymbol    the type symbol
     * @param ignoreError   whether to ignore errors
     * @return the type signature
     * @see #getTypeSignature(TypeSymbol, ModuleInfo)
     */
    public static String getTypeSignature(SemanticModel semanticModel, TypeSymbol typeSymbol, boolean ignoreError) {
        return getTypeSignature(semanticModel, typeSymbol, ignoreError, null);
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

            if (moduleInfo == null || !modPart.equals(moduleInfo.packageName())) {
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
            return withinDoClause(currentNode);
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

    //TODO: Remove this once the diagnostic helper is exposed to LS extensions
    public static Diagnostic transformBallerinaDiagnostic(io.ballerina.tools.diagnostics.Diagnostic diag) {
        DiagnosticInfo diagnosticInfo = diag.diagnosticInfo();
        return createDiagnostic(diag.message(), diag.location().lineRange(), diagnosticInfo.code(),
                diagnosticInfo.severity());
    }

    public static Diagnostic createDiagnostic(String message, LineRange lineRange, DiagnosticErrorCode errorCode) {
        return createDiagnostic(message, lineRange, errorCode.diagnosticId(), errorCode.severity());
    }

    public static Diagnostic createDiagnostic(String message, LineRange lineRange, String code,
                                              io.ballerina.tools.diagnostics.DiagnosticSeverity severity) {
        int startLine = lineRange.startLine().line();
        int startChar = lineRange.startLine().offset();
        int endLine = lineRange.endLine().line();
        int endChar = lineRange.endLine().offset();

        endLine = (endLine <= 0) ? startLine : endLine;
        endChar = (endChar <= 0) ? startChar + 1 : endChar;

        Range range = new Range(new Position(startLine, startChar), new Position(endLine, endChar));
        Diagnostic diagnostic = new Diagnostic(range, message, null, null, code);

        switch (severity) {
            case ERROR -> diagnostic.setSeverity(DiagnosticSeverity.Error);
            case WARNING -> diagnostic.setSeverity(DiagnosticSeverity.Warning);
            case HINT -> diagnostic.setSeverity(DiagnosticSeverity.Hint);
            case INFO -> diagnostic.setSeverity(DiagnosticSeverity.Information);
            default -> {
                // Ignored
            }
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
     * @param symbol     the symbol to check
     * @param moduleInfo the module descriptor of the current module
     * @return true if the symbol belongs to the default package, false otherwise
     * @see #isDefaultPackage(String, String, ModuleInfo)
     */
    public static boolean isDefaultPackage(Symbol symbol, ModuleInfo moduleInfo) {
        Optional<ModuleID> moduleId = symbol.getModule().map(ModuleSymbol::id);
        return moduleId.filter(
                moduleID -> isDefaultPackage(moduleID.orgName(), moduleID.moduleName(), moduleInfo)).isPresent();
    }

    /**
     * Checks if the given module is the default package.
     *
     * @param orgName     the organization name
     * @param packageName the package name
     * @param moduleInfo  the module descriptor of the current module
     * @return true if the module is the default package, false otherwise
     */
    public static boolean isDefaultPackage(String orgName, String packageName, ModuleInfo moduleInfo) {
        return (orgName.equals(moduleInfo.org()) && packageName.equals(moduleInfo.packageName()));
    }

    /**
     * Checks if the given line position is after another line position.
     *
     * @param position the line position to check
     * @param other    the other line position to compare against
     * @return true if the given line position is after the other line position, false otherwise
     */
    public static boolean isLinePositionAfter(LinePosition position, LinePosition other) {
        return position.line() > other.line() ||
                (position.line() == other.line() && position.offset() > other.offset());
    }

    /**
     * Checks if the given type name has a return type.
     *
     * @param typeName the type name to check
     * @return true if the type name has a return type, false otherwise
     */
    public static boolean hasReturn(String typeName) {
        return !typeName.equals("()");
    }

    /**
     * Generates the URI for the given source path.
     *
     * @param sourcePath the source path
     * @return the generated URI as a string
     */
    public static String getExprUri(String sourcePath) {
        String exprUriString = "expr" + Paths.get(sourcePath).toUri().toString().substring(4);
        return URI.create(exprUriString).toString();
    }

    /**
     * Generates a comma-separated list of import statements required for the given type symbol.
     *
     * @param typeSymbol the type symbol to analyze
     * @param moduleInfo the module information of the current module
     * @return an Optional containing comma-separated list of import statements, or empty if no imports needed
     */
    public static Optional<String> getImportStatements(TypeSymbol typeSymbol, ModuleInfo moduleInfo) {
        Set<String> imports = new HashSet<>();
        analyzeTypeSymbolForImports(imports, typeSymbol, moduleInfo);
        if (imports.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(",", imports));
    }

    private static void analyzeTypeSymbolForImports(Set<String> imports, TypeSymbol typeSymbol,
                                                    ModuleInfo moduleInfo) {
        switch (typeSymbol.typeKind()) {
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                unionTypeSymbol.memberTypeDescriptors()
                        .forEach(memberType -> analyzeTypeSymbolForImports(imports, memberType, moduleInfo));
            }
            case INTERSECTION -> {
                IntersectionTypeSymbol intersectionTypeSymbol = (IntersectionTypeSymbol) typeSymbol;
                intersectionTypeSymbol.memberTypeDescriptors()
                        .forEach(memberType -> analyzeTypeSymbolForImports(imports, memberType, moduleInfo));
            }
            case TABLE -> {
                TableTypeSymbol tableTypeSymbol = (TableTypeSymbol) typeSymbol;
                analyzeTypeSymbolForImports(imports, tableTypeSymbol.rowTypeParameter(), moduleInfo);
                tableTypeSymbol.keyConstraintTypeParameter()
                        .ifPresent(keyType -> analyzeTypeSymbolForImports(imports, keyType, moduleInfo));
            }
            case TUPLE -> {
                TupleTypeSymbol tupleTypeSymbol = (TupleTypeSymbol) typeSymbol;
                tupleTypeSymbol.memberTypeDescriptors()
                        .forEach(memberType -> analyzeTypeSymbolForImports(imports, memberType, moduleInfo));
            }
            case STREAM -> {
                StreamTypeSymbol streamTypeSymbol = (StreamTypeSymbol) typeSymbol;
                analyzeTypeSymbolForImports(imports, streamTypeSymbol.typeParameter(), moduleInfo);
                analyzeTypeSymbolForImports(imports, streamTypeSymbol.completionValueTypeParameter(), moduleInfo);
            }
            case FUTURE -> {
                FutureTypeSymbol futureTypeSymbol = (FutureTypeSymbol) typeSymbol;
                futureTypeSymbol.typeParameter()
                        .ifPresent(typeParam -> analyzeTypeSymbolForImports(imports, typeParam, moduleInfo));
            }
            case TYPEDESC -> {
                TypeDescTypeSymbol typeDescTypeSymbol = (TypeDescTypeSymbol) typeSymbol;
                typeDescTypeSymbol.typeParameter()
                        .ifPresent(typeParam -> analyzeTypeSymbolForImports(imports, typeParam, moduleInfo));
            }
            case ARRAY -> {
                ArrayTypeSymbol arrayTypeSymbol = (ArrayTypeSymbol) typeSymbol;
                analyzeTypeSymbolForImports(imports, arrayTypeSymbol.memberTypeDescriptor(), moduleInfo);
            }
            case MAP -> {
                MapTypeSymbol memberTypeSymbol = (MapTypeSymbol) typeSymbol;
                analyzeTypeSymbolForImports(imports, memberTypeSymbol.typeParam(), moduleInfo);
            }
            default -> {
                Optional<ModuleSymbol> moduleSymbol = typeSymbol.getModule();
                if (moduleSymbol.isEmpty()) {
                    return;
                }
                ModuleID moduleId = moduleSymbol.get().id();
                String orgName = moduleId.orgName();
                String packageName = moduleId.packageName();
                String moduleName = moduleId.moduleName();
                String modulePrefix = moduleId.modulePrefix();

                if (isPredefinedLangLib(orgName, packageName) || isAnnotationLangLib(orgName, packageName) ||
                        isWithinCurrentModule(moduleInfo, orgName, packageName, moduleName)) {
                    return;
                }

                // Check if this is within the current package
                if (orgName.equals(moduleInfo.org()) && modulePrefix.equals((moduleInfo.moduleName()))) {
                    imports.add(getImportStatement("", packageName, modulePrefix));
                } else {
                    imports.add(getImportStatement(orgName, packageName, moduleName));
                }
            }
        }
    }

    private static boolean isAnnotationLangLib(String orgName, String packageName) {
        return orgName.equals(CommonUtil.BALLERINA_ORG_NAME) && packageName.equals("lang.annotations");
    }

    /**
     * Generates the import statement  of the format `<org>/<package>[.<module>]`.
     *
     * @param orgName     the organization name
     * @param packageName the package name
     * @param moduleName  the module name
     * @return the import statement
     */
    public static String getImportStatement(String orgName, String packageName, String moduleName) {
        StringBuilder importStatement = new StringBuilder();
        if (!orgName.isEmpty()) {
            importStatement.append(orgName).append("/");
        }
        if (moduleName != null && moduleName.startsWith(packageName + ".")) {
            importStatement.append(moduleName);
        } else if (moduleName != null && !packageName.equals(moduleName)) {
            importStatement.append(packageName).append(".").append(moduleName);
        } else {
            importStatement.append(packageName);
        }
        return importStatement.toString();
    }

    /**
     * Checks if the given module is a predefined language library.
     *
     * @param orgName     the organization name
     * @param packageName the package name
     * @return true if the module is a predefined language library, false otherwise
     */
    public static boolean isPredefinedLangLib(String orgName, String packageName) {
        return orgName.equals(CommonUtil.BALLERINA_ORG_NAME) &&
                CommonUtil.PRE_DECLARED_LANG_LIBS.contains(packageName);
    }

    private static boolean isWithinCurrentModule(ModuleInfo defaultModuleInfo, String orgName, String packageName,
                                                 String moduleName) {
        return orgName.equals(defaultModuleInfo.org()) &&
                packageName.equals(defaultModuleInfo.packageName()) &&
                moduleName.equals(defaultModuleInfo.moduleName());
    }

    /**
     * Checks if the given symbol is within the given package.
     *
     * @param symbol     the symbol to check
     * @param moduleInfo the module descriptor of the current module
     * @return true if the symbol is within the given package, false otherwise
     */
    public static boolean isWithinPackage(Symbol symbol, ModuleInfo moduleInfo) {
        if (symbol.getModule().isEmpty()) {
            return false;
        }
        ModuleID moduleID = symbol.getModule().get().id();
        return moduleID.orgName().equals(moduleInfo.org()) &&
                moduleID.packageName().equals(moduleInfo.packageName());
    }

    /**
     * Converts a multi-line string into a formatted Ballerina documentation. Each line starts with a "#".
     *
     * @param text The input string.
     * @return The formatted Ballerina documentation string.
     */
    public static String convertToBalDocs(String text) {
        // Split the input text into lines
        String[] lines = text.split("\n");

        // Use StringBuilder for efficient string manipulation
        StringBuilder formattedComment = new StringBuilder();

        // Add "#" before each line and append to the result
        for (String line : lines) {
            formattedComment.append("# ").append(line).append("\n");
        }

        // Convert StringBuilder to String and return
        return formattedComment.toString();
    }

    /**
     * Checks if the given function symbol is a value language library function.
     *
     * @param functionSymbol the function symbol to check
     * @return true if the function symbol is a value language library function, false otherwise
     */
    public static boolean isValueLangLibFunction(FunctionSymbol functionSymbol) {
        Optional<ModuleSymbol> module = functionSymbol.getModule();
        if (module.isEmpty()) {
            return false;
        }
        ModuleID id = module.get().id();
        return id.orgName().equals(BALLERINA_ORG_NAME) && id.packageName().startsWith(LANG_LIB_PREFIX);
    }

    /**
     * Check whether the given symbol is a http module.
     *
     * @param symbol symbol to check
     * @return true if the symbol is a http module, false otherwise
     */
    public static boolean isHttpModule(Symbol symbol) {
        Optional<ModuleSymbol> module = symbol.getModule();
        if (module.isEmpty()) {
            return false;
        }

        ModuleID moduleId = module.get().id();
        return moduleId.orgName().equals(CommonUtils.BALLERINA_ORG_NAME) && moduleId.packageName().equals("http");
    }

    public static boolean isBallerinaNpModule(Symbol symbol) {
        Optional<ModuleSymbol> module = symbol.getModule();
        if (module.isEmpty()) {
            return false;
        }

        ModuleID moduleId = module.get().id();
        return moduleId.orgName().equals(CommonUtils.BALLERINA_ORG_NAME) && moduleId.packageName().equals("np");
    }

    public static boolean isNaturalExpressionBodiedFunction(SyntaxTree syntaxTree, FunctionSymbol functionSymbol) {
        if (functionSymbol.getLocation().isEmpty()) {
            return false;
        }
        NonTerminalNode node = getNode(syntaxTree, functionSymbol.getLocation().get().textRange());
        if (node.kind() != SyntaxKind.FUNCTION_DEFINITION) {
            return false;
        }
        FunctionDefinitionNode functionDefNode = (FunctionDefinitionNode) node;
        return isNaturalExpressionBodiedFunction(functionDefNode);
    }

    public static boolean isNaturalExpressionBodiedFunction(FunctionDefinitionNode functionDefNode) {
        FunctionBodyNode functionBody = functionDefNode.functionBody();
        return functionBody.kind() == SyntaxKind.EXPRESSION_FUNCTION_BODY
                && ((ExpressionFunctionBodyNode) functionBody).expression().kind() == SyntaxKind.NATURAL_EXPRESSION;
    }

    public static String getClassType(String packageName, String clientName) {
        String importPrefix = packageName.substring(packageName.lastIndexOf('.') + 1);
        return String.format("%s:%s", importPrefix, clientName);
    }

    /**
     * Returns the specified position if not null, or the end of the document otherwise.
     *
     * @param position The line position to check; if not null, it is returned as is
     * @param document The document to get the end position from if the position parameter is null
     * @return The original position if not null, or the line position at the end of the document
     */
    public static LinePosition getPosition(LinePosition position, Document document) {
        if (position != null) {
            return position;
        }

        // Get the end of the document
        TextDocument textDocument = document.textDocument();
        int textPosition = textDocument.toCharArray().length;
        return textDocument.linePositionFrom(textPosition);
    }

    /**
     * Extracts the default module prefix from a package name.
     * The prefix is the last segment of the package name after splitting by dots.
     *
     * @param packageName The fully qualified package name to extract the prefix from
     * @return The last segment of the package name as the default module prefix
     */
    public static String getDefaultModulePrefix(String packageName) {
        String[] parts = packageName.split("\\.");
        return parts.length > 0 ? parts[parts.length - 1] : packageName;
    }

    /**
     * Constructs a fully qualified ID from a module descriptor in the format:
     * <org>/<package-name>.<module-name>:<version>
     *
     * @param descriptor The module descriptor to construct ID from
     * @return The formatted ID string
     */
    public static String constructModuleId(ModuleDescriptor descriptor) {
        StringBuilder idBuilder = new StringBuilder();
        
        // Add organization
        idBuilder.append(descriptor.org().value()).append('/');
        
        // Add package name
        idBuilder.append(descriptor.name().packageName().value());
        
        // Add module name if it's not the default module
        String moduleNamePart = descriptor.name().moduleNamePart();
        if (moduleNamePart != null && !moduleNamePart.isEmpty()) {
            idBuilder.append('.').append(moduleNamePart);
        }
        
        // Add version
        idBuilder.append(':').append(descriptor.version());
        
        return idBuilder.toString();
    }

    /**
     * Checks whether the given import exists in the given module part node.
     *
     * @param node module part node
     * @param org organization name
     * @param module module name
     * @return true if the import exists, false otherwise
     */
    public static boolean importExists(ModulePartNode node, String org, String module) {
        return node.imports().stream().anyMatch(importDeclarationNode -> {
            String moduleName = importDeclarationNode.moduleName().stream()
                    .map(IdentifierToken::text)
                    .collect(Collectors.joining("."));
            return importDeclarationNode.orgName().isPresent() &&
                    org.equals(importDeclarationNode.orgName().get().orgName().text()) &&
                    module.equals(moduleName);
        });
    }
}
