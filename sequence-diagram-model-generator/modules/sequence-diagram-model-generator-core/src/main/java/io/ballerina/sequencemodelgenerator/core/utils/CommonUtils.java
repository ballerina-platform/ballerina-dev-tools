package io.ballerina.sequencemodelgenerator.core.utils;

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextRange;

/**
 * Util functions which will be used by the sequence model process.
 *
 * @since 2201.8.0
 */
public class CommonUtils {
    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        if (lineRange == null) {
            return null;
        }
        try {
            TextDocument textDocument = syntaxTree.textDocument();
            int start = textDocument.textPositionFrom(lineRange.startLine());
            int end = textDocument.textPositionFrom(lineRange.endLine());
            return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
