package io.ballerina.flowmodelgenerator.extension;

import io.ballerina.flowmodelgenerator.core.OpenAPIClientGenerator;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIClientGenerationRequest;
import io.ballerina.flowmodelgenerator.extension.response.OpenAPIClientGenerationResponse;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("openAPIService")
public class OpenAPIService implements ExtendedLanguageServerService {

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
    }

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<OpenAPIClientGenerationResponse> genClient(OpenAPIClientGenerationRequest req) {
        return CompletableFuture.supplyAsync(() -> {
            OpenAPIClientGenerationResponse response = new OpenAPIClientGenerationResponse();
            try {
                OpenAPIClientGenerator openAPIClientGenerator =
                        new OpenAPIClientGenerator(Path.of(req.openApiContractPath()), Path.of(req.projectPath()));
                response.setSource(openAPIClientGenerator.genClient(req.module()));
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }
}
