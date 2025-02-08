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
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorCompletionRequest;
import org.eclipse.lsp4j.CompletionContext;
import org.eclipse.lsp4j.CompletionItem;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for the expression editor completion service.
 *
 * @since 1.4.0
 */
public class ExpressionEditorCompletionTest extends AbstractLSTest {

    private static final Type COMPLETION_RESPONSE_TYPE = new TypeToken<List<CompletionItem>>() { }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());

        notifyDidOpen(sourcePath);
        ExpressionEditorCompletionRequest request = new ExpressionEditorCompletionRequest(sourcePath,
                testConfig.context(), testConfig.completionContext());
        JsonObject response = getResponse(request);
        List<CompletionItem> actualCompletions =
                gson.fromJson(response.get("left").getAsJsonArray(), COMPLETION_RESPONSE_TYPE);
        notifyDidClose(sourcePath);

        if (!assertArray("completions", actualCompletions, testConfig.completions())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.filePath(),
                    testConfig.context(), testConfig.completionContext(), actualCompletions);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String[] skipList() {
        // TODO: Remove after fixing https://github.com/ballerina-platform/ballerina-lang/issues/43706
        return new String[]{"proj6.json"};
    }

    @Test
    public void testMultipleRequests() throws IOException {
        Path configJsonPath = configDir.resolve("config.json");
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());

        notifyDidOpen(sourcePath);
        ExpressionEditorCompletionRequest request = new ExpressionEditorCompletionRequest(sourcePath,
                testConfig.context(), testConfig.completionContext());
        getResponse(request);

        ExpressionEditorContext.Info firstContext = testConfig.context();
        ExpressionEditorContext.Info info =
                new ExpressionEditorContext.Info("self.classVar > localVar + self. +  21", firstContext.startLine(),
                        32, firstContext.node(), firstContext.branch(), firstContext.property());
        ExpressionEditorCompletionRequest secondRequest = new ExpressionEditorCompletionRequest(sourcePath, info,
                testConfig.completionContext());
        JsonObject secondResponse = getResponse(secondRequest);
        List<CompletionItem> secondCompletions =
                gson.fromJson(secondResponse.get("left").getAsJsonArray(), COMPLETION_RESPONSE_TYPE);
        Assert.assertEquals(secondCompletions.size(), 1);
        Assert.assertEquals(secondCompletions.getFirst().getLabel(), "classVar");
        notifyDidClose(sourcePath);
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

    private record TestConfig(String description, String filePath, ExpressionEditorContext.Info context,
                              CompletionContext completionContext, List<CompletionItem> completions) {
    }
}
