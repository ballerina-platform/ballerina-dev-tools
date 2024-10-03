/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIServiceGenerationRequest;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the OpenAPI service generation.
 *
 * @since 1.4.0
 */
public class ServiceGeneratorTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("config1.json")},
//                {Path.of("config2.json")},
                {Path.of("config3.json")}
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Endpoint endpoint = TestUtil.newLanguageServer().withLanguageServer(new BallerinaLanguageServer()).build();
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        Path contractPath = resDir.resolve("contracts").resolve(testConfig.contractFile());

        Path project = configDir.resolve(config.getFileName().toString().split(".json")[0]);
        Files.createDirectories(project);
        String projectPath = project.toAbsolutePath().toString();
        OpenAPIServiceGenerationRequest request =
                new OpenAPIServiceGenerationRequest(contractPath.toAbsolutePath().toString(), projectPath, 9090);
        JsonObject resp = getResponse(endpoint, request);
        deleteFolder(project.toFile());
        if (!resp.getAsJsonObject("service").equals(testConfig.lineRange())) {
            TestConfig updatedConfig = new TestConfig(testConfig.contractFile(), resp.get("service").getAsJsonObject());
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
        TestUtil.shutdownLanguageServer(endpoint);
    }

    @Override
    protected String getResourceDir() {
        return "openapi_service_gen";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ServiceGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "generateServiceFromOpenApiContract";
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }

    /**
     * Represents the test configuration for the service generation.
     *
     * @param contractFile OpenAPI contract file
     * @param lineRange    line range of service declaration
     * @since 1.4.0
     */
    private record TestConfig(String contractFile, JsonObject lineRange) {

    }
}
