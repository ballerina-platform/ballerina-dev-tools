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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.core.expressioneditor.ExpressionEditorContext;
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorSignatureRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.SignatureHelpContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the expression editor signature help service.
 *
 * @since 1.4.0
 */
public class ExpressionEditorSignatureTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourcePath = getSourcePath(testConfig.filePath());

        notifyDidOpen(sourcePath);
        ExpressionEditorSignatureRequest request = new ExpressionEditorSignatureRequest(
                sourcePath, testConfig.context(), testConfig.signatureHelpContext());
        JsonObject response = getResponse(request);
        JsonElement actualSignatureHelp = response.get("signatures");
        notifyDidClose(sourcePath);

        if (!actualSignatureHelp.equals(testConfig.signatureHelp())) {
            TestConfig updatedConfig =
                    new TestConfig(testConfig.description(), testConfig.filePath(), testConfig.context(),
                            testConfig.signatureHelpContext(), actualSignatureHelp);
            compareJsonElements(actualSignatureHelp, testConfig.signatureHelp());
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "signature_help";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ExpressionEditorSignatureTest.class;
    }

    @Override
    protected String getApiName() {
        return "signatureHelp";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    private record TestConfig(String description, String filePath, ExpressionEditorContext.Info context,
                              SignatureHelpContext signatureHelpContext, JsonElement signatureHelp) {
    }
}
