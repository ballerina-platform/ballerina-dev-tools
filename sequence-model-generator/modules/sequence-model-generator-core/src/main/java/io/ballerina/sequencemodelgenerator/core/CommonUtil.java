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

package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Represents the common utility functions for the sequence diagram.
 *
 * @since 2.0.0
 */
public class CommonUtil {

    private CommonUtil() {
    }

    /**
     * Returns the type signature of the given type symbol.
     *
     * @param typeSymbol the type symbol
     * @return the type signature
     */
    public static String getTypeSignature(TypeSymbol typeSymbol) {
        return switch (typeSymbol.typeKind()) {
            case TYPE_REFERENCE -> {
                TypeReferenceTypeSymbol typeReferenceTypeSymbol = (TypeReferenceTypeSymbol) typeSymbol;
                String modulePrefix = getModuleName(typeReferenceTypeSymbol).map(s -> s + ":").orElse("");
                yield typeReferenceTypeSymbol.definition().getName()
                        .map(name -> modulePrefix + name)
                        .orElseGet(() -> getTypeSignature(typeReferenceTypeSymbol.typeDescriptor()));
            }
            case UNION -> {
                UnionTypeSymbol unionTypeSymbol = (UnionTypeSymbol) typeSymbol;
                yield unionTypeSymbol.memberTypeDescriptors().stream()
                        .map(CommonUtil::getTypeSignature)
                        .reduce((s1, s2) -> s1 + "|" + s2)
                        .orElse(unionTypeSymbol.signature());
            }
            case TYPEDESC -> {
                TypeDescTypeSymbol typeDescTypeSymbol = (TypeDescTypeSymbol) typeSymbol;
                yield typeDescTypeSymbol.typeParameter()
                        .map(CommonUtil::getTypeSignature)
                        .orElse(typeDescTypeSymbol.signature());
            }
            default -> {
                Optional<String> moduleName = typeSymbol.getModule().map(module -> module.id().modulePrefix());
                yield moduleName.map(s -> s + ":").orElse("") + typeSymbol.signature();
            }
        };
    }

    /**
     * Returns the syntax tree of the given file.
     *
     * @param project the project of the sequence diagram
     * @return the syntax tree of the file
     */
    public static SyntaxTree getSyntaxTree(Project project, Path filePath) {
        DocumentId documentId = project.documentId(filePath);
        return project.currentPackage().module(documentId.moduleId()).document(documentId).syntaxTree();
    }

    /**
     * Returns the file path of the given file name.
     *
     * @param project    the project of the sequence diagram
     * @param fileName   the file name
     * @param moduleName the module name in which the file resides
     * @return the file path
     */
    public static Path getFilePath(Project project, String fileName, String moduleName) {
        Path sourceRoot = project.sourceRoot();
        Path filePath = switch (project.kind()) {
            case SINGLE_FILE_PROJECT -> sourceRoot;
            case BUILD_PROJECT -> {
                Path tempFilePath = Path.of(fileName);
                if (tempFilePath.isAbsolute()) {
                    yield tempFilePath;
                }
                Path modulePath = moduleName == null || moduleName.equals(Constants.DEFAULT_MODULE) ?
                        sourceRoot : sourceRoot.resolve("modules").resolve(moduleName);
                yield modulePath.resolve(tempFilePath);
            }
            default -> throw new IllegalStateException("Unsupported project kind: " + project.kind());
        };
        return filePath.isAbsolute() ? filePath : sourceRoot.resolve(filePath);
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
     * Returns the module name of the given symbol.
     *
     * @param symbol the symbol to get the module name
     * @return the module name
     */
    public static Optional<String> getModuleName(Symbol symbol) {
        return symbol.getModule().map(module -> module.id().modulePrefix());
    }

    /**
     * Returns the semantic model of the given file.
     *
     * @param project  the project of the sequence diagram
     * @param filePath the file path
     * @return the semantic model of the file
     */
    public static SemanticModel getSemanticModel(Project project, Path filePath) {
        return project.currentPackage().getCompilation().getSemanticModel(project.documentId(filePath).moduleId());
    }
}
