package io.ballerina.sequencemodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.sequencemodelgenerator.core.exception.SequenceModelGenerationException;
import io.ballerina.sequencemodelgenerator.core.model.SequenceModel;
import io.ballerina.sequencemodelgenerator.core.visitors.RootNodeVisitor;
import io.ballerina.sequencemodelgenerator.core.visitors.VisitorContext;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;

import static io.ballerina.sequencemodelgenerator.core.Constants.INVALID_NODE_MSG;
import static io.ballerina.sequencemodelgenerator.core.utils.CommonUtils.findNode;

public class ModelGenerator {
    public SequenceModel getSequenceDiagramModel(Project project, LineRange position, SemanticModel semanticModel) throws SequenceModelGenerationException {
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
        if (node.isMissing()) {
            throw new SequenceModelGenerationException(INVALID_NODE_MSG);
        }

        // TODO: Move to separate function and handle exceptions
        VisitorContext visitorContext = new VisitorContext();
        RootNodeVisitor workerNodeVisitor = new RootNodeVisitor(semanticModel, packageName, visitorContext);
        node.accept(workerNodeVisitor);
        if (workerNodeVisitor.getModelGenerationException() != null) {
            throw workerNodeVisitor.getModelGenerationException();
        }
        return new SequenceModel(workerNodeVisitor.getVisitorContext().getParticipants(),position);
    }
}
