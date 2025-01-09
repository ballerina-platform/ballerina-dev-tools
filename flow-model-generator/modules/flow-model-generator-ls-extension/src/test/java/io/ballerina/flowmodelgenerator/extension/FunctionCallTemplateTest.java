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
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.core.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.flowmodelgenerator.extension.request.FunctionCallTemplateRequest;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for the function call template service.
 *
 * @since 2.0.0
 */
public class FunctionCallTemplateTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());

        notifyDidOpen(sourcePath);
        FunctionCallTemplateRequest request = new FunctionCallTemplateRequest(sourcePath, testConfig.codedata(),
                testConfig.kind());
        String template = getResponse(request).getAsJsonPrimitive("template").getAsString();
        notifyDidClose(sourcePath);

        if (!template.equals(testConfig.functionCall())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.filePath(),
                    testConfig.codedata(),
                    testConfig.kind(), template);
            // updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Test(dataProvider = "data-provider")
    public void testDiagnostics(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());

        // Call the function call template API
        notifyDidOpen(sourcePath);
        FunctionCallTemplateRequest request = new FunctionCallTemplateRequest(sourcePath, testConfig.codedata(),
                testConfig.kind());
        String template = getResponse(request).getAsJsonPrimitive("template").getAsString();

        // Call the diagnostics API 
        LinePosition startPosition = LinePosition.from(1, 0);
        int offset = template.indexOf("${1}");
        if (offset > -1) {
            template = template.replace("${1}", " ");
        }
        ExpressionEditorContext.Info info = new ExpressionEditorContext.Info(template, startPosition, offset,
                new JsonObject(), null, null);
        ExpressionEditorDiagnosticsRequest diagnosticsRequest =
                new ExpressionEditorDiagnosticsRequest(sourcePath, info);
        JsonObject response = getResponse(diagnosticsRequest, "expressionEditor/diagnostics");
        List<Diagnostic> diagnostics = gson.fromJson(response.get("diagnostics").getAsJsonArray(),
                new TypeToken<List<Diagnostic>>() { }.getType());
        notifyDidClose(sourcePath);

        // Check for undefined import/function diagnostic codes
        for (Diagnostic diagnostic : diagnostics) {
            String code = diagnostic.getCode().getLeft();
            Assert.assertFalse(UNDEFINED_DIAGNOSTICS_CODES.contains(code),
                    "Undefined diagnostic code found: " + code);
        }
    }

    @Override
    protected String getResourceDir() {
        return "function_call_template";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return FunctionCallTemplateTest.class;
    }

    @Override
    protected String getApiName() {
        return "functionCallTemplate";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    private record TestConfig(String description, String filePath, Codedata codedata,
                              FunctionCallTemplateRequest.FunctionCallTemplateKind kind, String functionCall) {

    }
}
