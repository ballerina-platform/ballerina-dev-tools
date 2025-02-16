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

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorDiagnosticsRequest;
import io.ballerina.flowmodelgenerator.extension.request.ImportModuleRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.eclipse.lsp4j.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * This is a test class that tests the module snippet API.
 *
 * @since 2.0.0
 */
public class ImportModuleTest extends AbstractLSTest {

    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());
        notifyDidOpen(sourcePath);

        // Send module snippet request
        ImportModuleRequest moduleSnippetRequest = new ImportModuleRequest(sourcePath, testConfig.importStatement());
        JsonObject successResponse = getResponse(moduleSnippetRequest);
        successResponse.get("success").getAsBoolean();
        Assert.assertTrue(successResponse.get("success").getAsBoolean(), "Module snippet request failed");

        // Send diagnostics request
        ExpressionEditorContext.Info info =
                new ExpressionEditorContext.Info(testConfig.expression(), LinePosition.from(1, 0),
                        0, new JsonObject(), null, null);
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
        return "import_module";
    }

    @Override
    protected String getApiName() {
        return "importModule";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ImportModuleTest.class;
    }

    private record TestConfig(String description, String filePath, String importStatement, String expression) {
    }
}
