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

package io.ballerina.designmodelgenerator.extension;

import io.ballerina.designmodelgenerator.core.DesignModelGenerator;
import io.ballerina.designmodelgenerator.core.model.DesignModel;
import io.ballerina.designmodelgenerator.extension.request.GetDesignModelRequest;
import io.ballerina.designmodelgenerator.extension.response.GetDesignModelResponse;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.Package;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("designModelService")
public class DesignModelGeneratorService implements ExtendedLanguageServerService {

    private final BuildOptions options = BuildOptions.builder()
            .setOffline(true)
            .setSticky(false)
            .build();

    @Override
    public Class<?> getRemoteInterface() {
        return null;
    }

    @JsonRequest
    public CompletableFuture<GetDesignModelResponse> getDesignModel(GetDesignModelRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            GetDesignModelResponse response = new GetDesignModelResponse();
            try {
                Path projectPath = Path.of(request.projectPath());

                // TODO: This is a temporary solution until we investigate why the workspace manager does not
                //  properly perform the package resolution
                // Ensure resolution and compilation are triggered
                Project project = ProjectLoader.loadProject(projectPath, options);
                Package currentPackage = project.currentPackage();
                currentPackage.getResolution();
                currentPackage.getCompilation();

                DesignModelGenerator designModelGenerator = new DesignModelGenerator(currentPackage);
                DesignModel designModel = designModelGenerator.generate();
                response.setDesignModel(designModel);
            } catch (Throwable e) {
                response.setError(e);
            }
            return response;
        });
    }
}
