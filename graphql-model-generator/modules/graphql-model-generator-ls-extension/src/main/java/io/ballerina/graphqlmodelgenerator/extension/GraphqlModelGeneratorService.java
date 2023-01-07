package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.graphqlmodelgenerator.core.ModelGenerator;
import io.ballerina.graphqlmodelgenerator.core.exception.SchemaFileGenerationException;
import io.ballerina.graphqlmodelgenerator.core.model.GraphqlModel;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("GraphqlDesignService")
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
            Path filePath = Path.of(request.getFilePath());
            Project project = null;
            try {
                project = getCurrentProject(filePath);
                PackageCompilation compilation = getPackageCompilation(project);
                System.out.println("===compilation completed");

                ModelGenerator modelGenerator = new ModelGenerator();
                GraphqlModel generatedModel = modelGenerator.getGraphqlModel(project,request.getLineRange());
                Gson gson = new GsonBuilder().serializeNulls().create();
                JsonObject graphqlModelJson = (JsonObject) gson.toJsonTree(generatedModel);
                Map<String, JsonObject> graphqlModelMap = new HashMap<>();
                graphqlModelMap.put("", graphqlModelJson);


                // ModelGenerator.getGraphqlModel(project,request.getLineRange());

            } catch (WorkspaceDocumentException e) {
                e.printStackTrace();
            } catch (EventSyncException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SchemaFileGenerationException e) {
                e.printStackTrace();
            }

//            Map<String, JsonObject> componentModelMap = new HashMap<>();
//            for (String documentUri : request.getDocumentUris()) {
//                Path path = Path.of(documentUri);
//                try {
//                    Project project = getCurrentProject(path);
//                    PackageCompilation compilation = getPackageCompilation(project);
//                    System.out.println("===test");
//                    if (!Utils.modelAlreadyExists(componentModelMap, project.currentPackage())) {
//                        ComponentModelBuilder componentModelBuilder = new ComponentModelBuilder();
//                        ComponentModel projectModel = componentModelBuilder
//                                .constructComponentModel(project.currentPackage());
//                        Gson gson = new GsonBuilder().serializeNulls().create();
//                        JsonObject componentModelJson = (JsonObject) gson.toJsonTree(projectModel);
//                        componentModelMap.put(Utils.getQualifiedPackageName(
//                                projectModel.getPackageId()), componentModelJson);
//                    }
//                } catch (ComponentModelException | WorkspaceDocumentException | EventSyncException e) {
//                    // todo : Improve error messages
//                    DiagnosticMessage message = DiagnosticMessage.componentModellingService001(documentUri);
//                    response.addDiagnostics
//                            (DiagnosticUtils.getDiagnosticResponse(List.of(message), response));
//                } catch (Exception e) {
//                    DiagnosticMessage message = DiagnosticMessage.componentModellingService002(
//                            e.getMessage(), Arrays.toString(e.getStackTrace()), documentUri);
//                    response.addDiagnostics
//                            (DiagnosticUtils.getDiagnosticResponse(List.of(message), response));
//                }
//            }
//            response.setComponentModels(componentModelMap);
            return response;
        });
    }

    private static PackageCompilation getPackageCompilation(Project project) throws IOException {
        DiagnosticResult diagnosticResult = project.currentPackage().runCodeGenAndModifyPlugins();
        boolean hasErrors = diagnosticResult
                .diagnostics().stream()
                .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
        if (!hasErrors) {
            PackageCompilation compilation = project.currentPackage().getCompilation();
            hasErrors = compilation.diagnosticResult()
                    .diagnostics().stream()
                    .anyMatch(d -> DiagnosticSeverity.ERROR.equals(d.diagnosticInfo().severity()));
            if (!hasErrors) {
                return compilation;
            }
        }
        throw new IOException();
    }

    private Project getCurrentProject(Path path) throws WorkspaceDocumentException,
            EventSyncException {

        Optional<Project> project = workspaceManager.project(path);
        if (project.isEmpty()) {
            return workspaceManager.loadProject(path);
        }
        return project.get();
    }
}
