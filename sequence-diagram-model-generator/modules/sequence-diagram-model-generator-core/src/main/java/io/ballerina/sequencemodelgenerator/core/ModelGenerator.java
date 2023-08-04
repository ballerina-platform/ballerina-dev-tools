package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.*;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.sequencemodelgenerator.core.model.SequenceModel;
import io.ballerina.sequencemodelgenerator.core.visitors.EntryNodeVisitor;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.Optional;

import static io.ballerina.sequencemodelgenerator.core.utils.CommonUtils.findNode;

public class ModelGenerator {
    public SequenceModel getSequenceDiagramModel(Project project, LineRange position, SemanticModel semanticModel) {
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            Path filePath = Path.of(position.fileName());
            docId = project.documentId(filePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            Module currentModule = packageName.getDefaultModule();
            docId = currentModule.documentIds().iterator().next();
            doc = currentModule.document(docId);
        }

        SyntaxTree syntaxTree = doc.syntaxTree();
        NonTerminalNode node = findNode(syntaxTree, position);
        Optional<Symbol> clientSymbol = semanticModel.symbol(node);
        EntryNodeVisitor workerNodeVisitor = new EntryNodeVisitor(semanticModel, packageName);
        node.accept(workerNodeVisitor);
        workerNodeVisitor.getParticipants();



        SequenceModel sequenceModel = new SequenceModel(workerNodeVisitor.getParticipants(), workerNodeVisitor.getInteractions());


        return sequenceModel;
    }
}
