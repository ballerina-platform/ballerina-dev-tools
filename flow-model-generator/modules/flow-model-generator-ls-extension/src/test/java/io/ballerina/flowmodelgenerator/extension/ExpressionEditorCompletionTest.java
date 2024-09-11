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
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for the expression editor completion service.
 *
 * @since 1.4.0
 */
public class ExpressionEditorCompletionTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        ExpressionEditorCompletionRequest request = new ExpressionEditorCompletionRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString(), testConfig.expression(),
                testConfig.branch(), testConfig.property(), testConfig.startLine(), testConfig.offset(),
                testConfig.context(), testConfig.node());
        JsonObject response = getResponse(request);

        List<CompletionItem> actualCompletions = gson.fromJson(response.get("left").getAsJsonArray(),
                new TypeToken<List<CompletionItem>>() { }.getType());
        if (!assertArray("completions", actualCompletions, testConfig.completions())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.filePath(),
                    testConfig.expression(), testConfig.branch(), testConfig.property(), testConfig.startLine(),
                    testConfig.offset(), testConfig.context(), testConfig.node(), actualCompletions);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "completions";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ExpressionEditorCompletionTest.class;
    }

    @Override
    protected String getApiName() {
        return "completion";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    private record TestConfig(String description, String filePath, String expression, String branch, String property,
                              LinePosition startLine, int offset, CompletionContext context, JsonObject node,
                              List<CompletionItem> completions) {
    }
}
