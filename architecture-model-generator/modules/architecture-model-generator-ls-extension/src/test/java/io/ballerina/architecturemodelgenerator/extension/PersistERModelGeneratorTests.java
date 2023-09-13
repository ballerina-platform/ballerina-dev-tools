/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.architecturemodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.architecturemodelgenerator.core.ArchitectureModel;
import io.ballerina.architecturemodelgenerator.extension.persist.PersistERModelRequest;
import io.ballerina.architecturemodelgenerator.extension.persist.PersistERModelResponse;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test Persist ER Model generation.
 *
 * @since 2201.6.0
 */
public class PersistERModelGeneratorTests {

    private static final Path RES_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String BALLERINA = "ballerina";
    private static final String RESULTS = "results";
    private static final String PERSIST_ER_MODEL_SERVICE = "persistERGeneratorService/getPersistERModels";
    private Endpoint serviceEndpoint;
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @BeforeClass
    public void startLanguageServer() {
        this.serviceEndpoint = TestUtil.initializeLanguageSever();
    }

    @Test(description = "Test Persist ER Model generation", enabled = false)
    public void testPersistERModelGeneration() throws IOException, ExecutionException, InterruptedException {

        Path balFile = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("rainier/persist", "rainier.bal").toString());
        Path expectedJsonPath = RES_DIR.resolve(RESULTS).resolve(Path.of("rainier_er_model.json"));

        PersistERModelRequest request = new PersistERModelRequest();
        request.setDocumentUri(balFile.toString());

        CompletableFuture<?> result = serviceEndpoint.request(PERSIST_ER_MODEL_SERVICE, request);
        PersistERModelResponse response = (PersistERModelResponse) result.get();

        JsonObject generatedJsonObject = response.getPersistERModels();
        ArchitectureModel generatedModel = gson.fromJson(generatedJsonObject, ArchitectureModel.class);
        ArchitectureModel expectedModel = getEntityModels(expectedJsonPath);

        String generatedERModelString = TestUtils.replaceStdLibVersionStrings(gson.toJson(generatedModel.getEntities())
                .replaceAll("\\s+", "")
                .replaceAll("\\\\\\\\", "/"));
        String expectedERModelString = TestUtils.replaceStdLibVersionStrings(gson.toJson(expectedModel.getEntities())
                .replaceAll("\\s+", "")
                .replaceAll("\\{srcPath}", RES_DIR.toString().replaceAll("\\\\", "/")));
        Assert.assertEquals(generatedERModelString, expectedERModelString);
    }

    public static ArchitectureModel getEntityModels(Path expectedFilePath) throws IOException {
        Stream<String> lines = Files.lines(expectedFilePath);
        String content = lines.collect(Collectors.joining(System.lineSeparator()));
        lines.close();
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.fromJson(content, ArchitectureModel.class);
    }
}
