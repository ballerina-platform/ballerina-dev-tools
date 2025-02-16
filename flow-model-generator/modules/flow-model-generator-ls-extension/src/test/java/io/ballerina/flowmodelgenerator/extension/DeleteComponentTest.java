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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.ComponentDeleteRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the flow model source generator service.
 *
 * @since 1.4.0
 */
public class DeleteComponentTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<Map<String, List<TextEdit>>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String sourcePath = getSourcePath(testConfig.filePath());
        ComponentDeleteRequest deleteRequest = new ComponentDeleteRequest(sourcePath, gson.toJsonTree(testConfig));

        JsonObject deleteResponse = getResponse(deleteRequest).getAsJsonObject("textEdits");
        Map<String, List<TextEdit>> actualTextEdits = gson.fromJson(deleteResponse, textEditListType);

        assertTextEdits(actualTextEdits, testConfig, configJsonPath);
    }

    private void assertTextEdits(Map<String, List<TextEdit>> actualTextEdits,
                                 TestConfig testConfig, Path configJsonPath) throws IOException {
        boolean assertFailure = false;
        Map<String, List<TextEdit>> newMap = new HashMap<>();
        for (Map.Entry<String, List<TextEdit>> entry : actualTextEdits.entrySet()) {
            Path fullPath = Paths.get(entry.getKey());
            String relativePath = sourceDir.relativize(fullPath).toString();

            List<TextEdit> textEdits = testConfig.output().get(relativePath.replace("\\", "/"));
            if (textEdits == null) {
                log.info("No text edits found for the file: " + relativePath);
                assertFailure = true;
            } else if (!assertArray("text edits", entry.getValue(), textEdits)) {
                assertFailure = true;
            }

            newMap.put(relativePath, entry.getValue());
        }

        if (assertFailure) {
            TestConfig updatedConfig =
                    new TestConfig(testConfig.description(), testConfig.filePath(), testConfig.startLine(),
                            testConfig.startColumn(), testConfig.endLine(), testConfig.endColumn(), newMap);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }


    @Override
    protected String getResourceDir() {
        return "delete_component";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DeleteComponentTest.class;
    }

    @Override
    protected String getApiName() {
        return "deleteComponent";
    }

    /**
     * Represents the test configuration for the delete node test.
     *
     * @param description The description of the test.
     * @param filePath    The path to the source file that contains the nodes to be deleted.
     * @param startLine   The starting line number of the node to be deleted.
     * @param startColumn The starting column position of the node to be deleted.
     * @param endLine     The ending line number of the node to be deleted.
     * @param endColumn   The ending column position of the node to be deleted.
     * @param output      The expected output.
     */
    private record TestConfig(String description, String filePath,
                              int startLine, int startColumn, int endLine, int endColumn,
                              Map<String, List<TextEdit>> output) {
    }
}
