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
import io.ballerina.flowmodelgenerator.extension.request.FlowModelNodeTemplateRequest;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorRequest;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIClientGenerationRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenApiClientGeneratorTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

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
                getResponse(openAPIClientReq).getAsJsonObject("source").get("textEditsMap");

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

        String filePath = project.resolve(testConfig.source()).toAbsolutePath().toString();

        FlowModelNodeTemplateRequest nodeTemplateRequest =
                new FlowModelNodeTemplateRequest(filePath, testConfig.position(), testConfig.codedata());
        JsonElement nodeTemplate = getResponse(nodeTemplateRequest, "flowDesignService/getNodeTemplate").get(
                "flowNode");

        FlowModelSourceGeneratorRequest sourceRequest = new FlowModelSourceGeneratorRequest(filePath, nodeTemplate);
        JsonObject connectionSource =
                getResponse(sourceRequest, "flowDesignService/getSourceCode").getAsJsonObject("textEdits");

        Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(connectionSource, textEditListType);
        boolean assertFailure = false;
        if (actualTextEdits.size() != testConfig.textEdits().size()) {
            log.info("The number of text edits does not match the expected output.");
            assertFailure = true;
        }

        Map<String, List<TextEdit>> newMap = new HashMap<>();
        for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
            Path fullPath = Paths.get(entry.getKey());
            String relativePath = project.relativize(fullPath).toString();

            List<TextEdit> newTextEdits = testConfig.textEdits().get(relativePath.replace("\\", "/"));
            if (newTextEdits == null) {
                log.info("No text edits found for the file: " + relativePath);
                assertFailure = true;
            } else if (!assertArray("text edits", entry.getValue(), newTextEdits)) {
                assertFailure = true;
            }

            newMap.put(relativePath, entry.getValue());
        }

        deleteFolder(project.toFile());
        if (!nodeTemplate.equals(testConfig.output()) || assertFailure) {
            TestConfig updatedConfig =
                    new TestConfig(testConfig.contractFile(), testConfig.balToml(), testConfig.module(),
                            testConfig.source(), testConfig.position(), testConfig.description(),
                            testConfig.codedata(), nodeTemplate, newMap);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
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
        return "openapi_client_gen";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return OpenApiClientGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "genClient";
    }

    @Override
    protected String getServiceName() {
        return "openAPIService";
    }

    /**
     * Represents the test configuration for the service generation.
     *
     * @param contractFile OpenAPI contract file
     * @param balToml      Ballerina.toml content
     * @param module       Module name
     * @param source       Source file name
     * @param position     Line position
     * @param description  Test description
     * @param codedata     Codedata of the node
     * @param output       Expected output
     * @param textEdits    Text edits
     * @since 1.4.0
     */
    private record TestConfig(String contractFile, String balToml, String module, String source,
                              LinePosition position, String description, JsonObject codedata, JsonElement output,
                              Map<String, List<TextEdit>> textEdits) {

    }
}
