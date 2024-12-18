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

import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.extension.request.FunctionCallTemplateRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the function call template service.
 *
 * @since 1.4.0
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
            TestConfig updatedConfig =
                    new TestConfig(testConfig.description(), testConfig.filePath(), testConfig.codedata(),
                            testConfig.kind(), template);
            // updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
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
