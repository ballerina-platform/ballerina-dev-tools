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
import io.ballerina.flowmodelgenerator.extension.request.ExpressionEditorTypesRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.CompletionItem;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests for the expression editor types service.
 *
 * @since 1.4.0
 */
public class ExpressionEditorTypesTest extends AbstractLSTest {

    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        ExpressionEditorTypesRequest request =
                new ExpressionEditorTypesRequest(getSourcePath(testConfig.source()), testConfig.typeConstraint());
        JsonObject response = getResponse(request);

        List<CompletionItem> actualCompletions = gson.fromJson(response.get("left").getAsJsonArray(),
                ExpressionEditorCompletionTest.COMPLETION_RESPONSE_TYPE);
        if (!assertArray("completions", actualCompletions, testConfig.completions())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.source(),
                    testConfig.typeConstraint(), actualCompletions);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "types";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ExpressionEditorTypesTest.class;
    }

    @Override
    protected String getApiName() {
        return "types";
    }

    @Override
    protected String getServiceName() {
        return "expressionEditor";
    }

    private record TestConfig(String description, String source, String typeConstraint,
                              List<CompletionItem> completions) {
    }
}
