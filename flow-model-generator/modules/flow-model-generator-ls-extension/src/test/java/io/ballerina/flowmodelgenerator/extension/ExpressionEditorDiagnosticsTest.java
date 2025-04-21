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
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for the expression editor diagnostics service.
 *
 * @since 1.4.0
 */
public class ExpressionEditorDiagnosticsTest extends AbstractLSTest {

    private static final Type diagnosticsType = new TypeToken<List<Diagnostic>>() { }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());

        notifyDidOpen(sourcePath);
        ExpressionEditorDiagnosticsRequest request =
                new ExpressionEditorDiagnosticsRequest(sourcePath, testConfig.context());
        JsonObject response = getResponse(request);
        List<Diagnostic> actualDiagnostics = gson.fromJson(response.get("diagnostics").getAsJsonArray(),
                diagnosticsType);
        notifyDidClose(sourcePath);

        if (!assertArray("diagnostics", actualDiagnostics, testConfig.diagnostics())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.filePath(),
                    testConfig.context(), actualDiagnostics);
            // updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String[] skipList() {
        return new String[]{
                // TODO: Provide diagnostics when assigning a value to type
                "assign2.json",
                // TODO: Fix the failing test case in Windows
                "variable6.json"
        };
    }

    @Test
    public void testMultipleRequests() throws IOException, InterruptedException {
        // Load the template test config
        Path configJsonPath = configDir.resolve("variable1.json");
        TestConfig templateTestConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        ExpressionEditorContext.Info templateContext = templateTestConfig.context();
        String sourcePath = getSourcePath(templateTestConfig.filePath());
        String method = getServiceName() + "/" + getApiName();
        notifyDidOpen(sourcePath);

        // Fire multiple requests to gradually build the expression "fn({id: 0})"
        String[] expressionSteps = {
                "f",
                "fn",
                "fn(",
                "fn({",
                "fn({id:",
                "fn({id: 0",
        };
        for (int i = 0; i < expressionSteps.length - 1; i++) {
            ExpressionEditorContext.Info context = new ExpressionEditorContext.Info(expressionSteps[i],
                    templateContext.startLine(), templateContext.offset(), templateContext.lineOffset(),
                    templateContext.codedata(), templateContext.property());
            ExpressionEditorDiagnosticsRequest req = new ExpressionEditorDiagnosticsRequest(sourcePath, context);
            serviceEndpoint.request(method, req);
            Thread.sleep(400);
        }

        // In the final complete expression, assert that no diagnostics are returned
        ExpressionEditorContext.Info context =
                new ExpressionEditorContext.Info("fn({id: 0})", templateContext.startLine(), templateContext.offset(),
                        templateContext.lineOffset(), templateContext.codedata(), templateContext.property());
        ExpressionEditorDiagnosticsRequest req = new ExpressionEditorDiagnosticsRequest(sourcePath, context);
        JsonObject resp = getResponse(req);
        List<Diagnostic> diagnostics = gson.fromJson(resp.get("diagnostics").getAsJsonArray(), diagnosticsType);
        notifyDidClose(sourcePath);
        Assert.assertTrue(diagnostics.isEmpty(), "Expected no diagnostics for complete expression");
    }

    @Override
    protected String getResourceDir() {
        return "diagnostics";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ExpressionEditorDiagnosticsTest.class;
    }

    @Override
    protected String getApiName() {
        return "diagnostics";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    private record TestConfig(String description, String filePath, ExpressionEditorContext.Info context,
                              List<Diagnostic> diagnostics) {
    }
}
