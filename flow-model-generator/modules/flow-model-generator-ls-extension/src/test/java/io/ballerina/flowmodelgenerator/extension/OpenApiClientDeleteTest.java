/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIClientDeleteRequest;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIClientGenerationRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpenApiClientDeleteTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("config1.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        Path contractPath = resDir.resolve("contracts").resolve(testConfig.contractFile());

        Path project = configDir.resolve(config.getFileName().toString().split(".json")[0]);
        Files.createDirectories(project);
        Path balToml = project.resolve("Ballerina.toml");
        Files.createFile(balToml);
        Files.writeString(balToml, testConfig.balToml());
        String projectPath = project.toAbsolutePath().toString();
        String module = testConfig.module();
        OpenAPIClientGenerationRequest openAPIClientReq =
                new OpenAPIClientGenerationRequest(contractPath.toAbsolutePath().toString(), projectPath, module);
        JsonElement openAPIClientSource =
                getResponse(openAPIClientReq, getServiceName() + "/genClient")
                        .getAsJsonObject("source").get("textEditsMap");

        Path modulePath = project.resolve("generated").resolve(module);
        Files.createDirectories(modulePath);
        Files.createFile(project.resolve(testConfig.source()));
        Files.createFile(project.resolve("connections.bal"));
        Map<String, List<TextEdit>> textEdits = gson.fromJson(openAPIClientSource, textEditListType);
        for (Map.Entry<String, List<TextEdit>> entry : textEdits.entrySet()) {
            Path filePath = project.resolve(entry.getKey());
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
            StringBuilder content = new StringBuilder(Files.readString(filePath));
            List<TextEdit> edits = entry.getValue();
            for (TextEdit edit : edits) {
                content.append(edit.getNewText());
            }
            Files.writeString(filePath, content);
        }

        OpenAPIClientDeleteRequest openAPIClientDeleteRequest = new OpenAPIClientDeleteRequest(projectPath, module);
        JsonObject response = getResponse(openAPIClientDeleteRequest).getAsJsonObject("deleteData");
        response.getAsJsonArray("filesToDelete");
        List<String> paths = gson.fromJson(response.getAsJsonArray("filesToDelete"), new TypeToken<List<String>>() {
        }.getType());

        List<String> fileNames = new ArrayList<>();
        for (String path : paths) {
            fileNames.add(Path.of(path).getFileName().toString());
        }

        deleteFolder(project.toFile());
        if (!fileNames.equals(testConfig.files)) {
            TestConfig updatedConfig = new TestConfig(testConfig.contractFile(), testConfig.balToml(),
                    testConfig.module(), testConfig.source(),
                    gson.fromJson(response.getAsJsonObject("textEditsMap"), textEditListType), fileNames);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: (%s)", configJsonPath));
        }
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

    @Override
    protected String getResourceDir() {
        return "openapi_client_delete";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return OpenApiClientDeleteTest.class;
    }

    @Override
    protected String getApiName() {
        return "deleteModule";
    }

    @Override
    protected String getServiceName() {
        return "openAPIService";
    }

    /**
     * Represents the test configuration for open api client generation.
     *
     * @param contractFile OpenAPI contract file
     * @param balToml      Ballerina.toml content
     * @param module       Module name
     * @param source       Source file name
     * @param textEdits    Text edits
     * @param files        Files to delete
     * @since 1.4.0
     */
    private record TestConfig(String contractFile, String balToml, String module, String source,
                              Map<String, List<TextEdit>> textEdits, List<String> files) {

    }
}
