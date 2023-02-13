package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.graphqlmodelgenerator.core.ModelGenerator;
import io.ballerina.graphqlmodelgenerator.core.exception.GraphqlModelGenerationException;
import io.ballerina.graphqlmodelgenerator.core.model.GraphqlModel;
import io.ballerina.projects.Project;
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

import static io.ballerina.graphqlmodelgenerator.core.Constants.EMPTY_SEMANTIC_MODEL_MSG;
import static io.ballerina.graphqlmodelgenerator.core.Constants.UNEXPECTED_ERROR_MSG;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("graphqlDesignService")
public class GraphqlModelGeneratorService implements ExtendedLanguageServerService {

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
    public CompletableFuture<GraphqlDesignServiceResponse> getGraphqlModel
            (GraphqlDesignServiceRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            GraphqlDesignServiceResponse response = new GraphqlDesignServiceResponse();
            try {
                Path filePath = Path.of(request.getFilePath());
                Project project = getCurrentProject(filePath);
                if (this.workspaceManager.semanticModel(filePath).isEmpty()){
                    throw new GraphqlModelGenerationException(EMPTY_SEMANTIC_MODEL_MSG);
                }
                SemanticModel semanticModel = this.workspaceManager.semanticModel(filePath).get();

                ModelGenerator modelGenerator = new ModelGenerator();
                GraphqlModel generatedModel = modelGenerator.getGraphqlModel(project, request.getLineRange(),
                        semanticModel);
                Gson gson = new GsonBuilder().serializeNulls().create();
                JsonElement graphqlModelJson = gson.toJsonTree(generatedModel);
                response.setGraphqlDesignModel(graphqlModelJson);
            } catch (WorkspaceDocumentException |  EventSyncException | GraphqlModelGenerationException e){
                response.setIncompleteModel(true);
                response.setErrorMsg(e.getMessage());
            } catch (Exception e){
                response.setIncompleteModel(true);
                response.setErrorMsg(String.format(UNEXPECTED_ERROR_MSG, e.getMessage()));
            }
            return response;
        });
    }

//    private static PackageCompilation getPackageCompilation(Project project) throws IOException {
//        DiagnosticResult diagnosticResult = project.currentPackage().runCodeGenAndModifyPlugins();
//        boolean hasErrors = diagnosticResult
//                .diagnostics().stream()
//                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
//        if (!hasErrors) {
//            PackageCompilation compilation = project.currentPackage().getCompilation();
//            hasErrors = compilation.diagnosticResult()
//                    .diagnostics().stream()
//                    .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
//            if (!hasErrors) {
//                return compilation;
//            }
//        }
//        throw new IOException();
//    }

    private Project getCurrentProject(Path path) throws WorkspaceDocumentException,
            EventSyncException {

        Optional<Project> project = workspaceManager.project(path);
        if (project.isEmpty()) {
            return workspaceManager.loadProject(path);
        }
        return project.get();
    }
}
