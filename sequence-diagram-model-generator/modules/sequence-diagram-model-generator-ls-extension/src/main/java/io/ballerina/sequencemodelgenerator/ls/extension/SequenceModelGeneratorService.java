package io.ballerina.sequencemodelgenerator.ls.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Project;
import io.ballerina.sequencemodelgenerator.core.ModelGenerator;
import io.ballerina.sequencemodelgenerator.core.model.SequenceModel;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("sequenceModelGeneratorService")
public class SequenceModelGeneratorService implements ExtendedLanguageServerService {
    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return getClass();
    }

    @JsonRequest
    public CompletableFuture<SequenceDiagramServiceResponse> getSequenceDiagramModel(SequenceDiagramServiceRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            SequenceDiagramServiceResponse response = new SequenceDiagramServiceResponse();
            try {
                Path filePath = Path.of(request.getFilePath());
                Project project = getCurrentProject(filePath);
                if (this.workspaceManager.semanticModel(filePath).isEmpty()) {
                    System.out.println("empty semantic model");
                }
                SemanticModel semanticModel = this.workspaceManager.semanticModel(filePath).get();
                ModelGenerator modelGenerator = new ModelGenerator();
                SequenceModel sequenceModel= modelGenerator.getSequenceDiagramModel(project, request.getLineRange(), semanticModel);
                Gson gson = new GsonBuilder().serializeNulls().create();
                JsonElement sequenceModelJson = gson.toJsonTree(sequenceModel);
                System.out.println(sequenceModelJson);
                response.setSequenceDiagramModel(sequenceModelJson);

            } catch (Exception e) {
//                response.setIncompleteModel(true);
//                response.setErrorMsg(String.format(UNEXPECTED_ERROR_MSG, e.getMessage()));
                System.out.println(e);
            }


            return response;
        });
    }

    private Project getCurrentProject(Path path) throws WorkspaceDocumentException, EventSyncException {
        Optional<Project> project = workspaceManager.project(path);
        if (project.isEmpty()) {
            return workspaceManager.loadProject(path);
        }
        return project.get();
    }

}
