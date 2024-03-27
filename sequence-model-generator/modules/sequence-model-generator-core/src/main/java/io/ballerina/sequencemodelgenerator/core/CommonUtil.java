package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.TextRange;

import java.nio.file.Path;
import java.util.Optional;

public class CommonUtil {

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
                yield getTypeSignature(typeReferenceTypeSymbol.typeDescriptor());
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

    public static SyntaxTree getSyntaxTree(Project project, String fileName, String moduleName) {
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
        Path absPath = filePath.isAbsolute() ? filePath : sourceRoot.resolve(filePath);
        DocumentId documentId = project.documentId(absPath);
        Module module = project.currentPackage().module(documentId.moduleId());
        return module.document(documentId).syntaxTree();
    }

    public static NonTerminalNode getNode(SyntaxTree syntaxTree, TextRange textRange) {
        ModulePartNode modulePartNode = syntaxTree.rootNode();
        return modulePartNode.findNode(textRange, true);
    }

    public static Optional<String> getModuleName(Symbol symbol) {
        return symbol.getModule().map(module -> module.id().modulePrefix());
    }
}
