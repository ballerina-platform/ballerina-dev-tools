package io.ballerina.graphqlmodelgenerator.core.utils;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;


public class ModelGenerationUtils {
//    public Node getNode (LineRange nodePosition){
//        String fileUri = nodePosition.filePath();
//        Optional<SyntaxTree> syntaxTree = getPathFromURI(fileUri).flatMap(workspaceManager::syntaxTree);
//    }
//
//    public static Optional<Path> getPathFromURI(String uri) {
//        try {
//            return Optional.of(Paths.get(new URL(uri).toURI()));
//        } catch (URISyntaxException | MalformedURLException ignore) {
//        }
//        return Optional.empty();
//    }
}
