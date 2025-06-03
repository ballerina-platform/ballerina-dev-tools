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
import io.ballerina.flowmodelgenerator.extension.request.ConfigVariableUpdateRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Test class for 'updateConfigVariable()' API in config API V2.
 *
 * @since 2.0.0
 */
public class ConfigVariablesV2UpdateTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        ConfigVariableUpdateTestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath),
                ConfigVariableUpdateTestConfig.class);

        String projectPath = sourceDir.resolve(testConfig.project()).toAbsolutePath().toString();

        ConfigVariableUpdateRequest request = new ConfigVariableUpdateRequest(
                testConfig.request().packageName(),
                testConfig.request().moduleName(),
                Paths.get(projectPath, testConfig.request().configFilePath()).toAbsolutePath().toString(),
                testConfig.request().configVariable()
        );
        ConfigVariableUpdateResponse actualResponse = gson.fromJson(getResponse(request), ConfigVariableUpdateResponse.class);

        if (!isEqual(testConfig.response().textEdits(), actualResponse.textEdits())) {
//            updateConfig(configJsonPath, new ConfigVariableUpdateTestConfig(
//                    testConfig.description(),
//                    testConfig.project(),
//                    testConfig.request(),
//                    new Response(actualResponse.textEdits()))
//            );
            Assert.fail(String.format("Failed test: '%s'", configJsonPath));
        }
    }

    private boolean isEqual(Map<String, TextEdit[]> expected, Map<String, TextEdit[]> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }

        for (Map.Entry<String, TextEdit[]> entry : expected.entrySet()) {
            String key = entry.getKey();
            TextEdit[] expectedEdits = entry.getValue();
            TextEdit[] actualEdits = actual.get(key);
            if (actualEdits == null || expectedEdits.length != actualEdits.length) {
                return false;
            }
            for (int i = 0; i < expectedEdits.length; i++) {
                if (!expectedEdits[i].equals(actualEdits[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected String getResourceDir() {
        return "configurable_variables_v2_update";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ConfigVariablesV2UpdateTest.class;
    }

    @Override
    protected String getApiName() {
        return "updateConfigVariable";
    }

    @Override
    protected String getServiceName() {
        return "configEditorV2";
    }

    private record ConfigVariableUpdateResponse(Map<String, TextEdit[]> textEdits) {

    }

    private record ConfigVariableUpdateTestConfig(String description, String project, Request request,
                                                  Response response) {

    }

    private record Request(String packageName, String moduleName, String configFilePath, JsonElement configVariable) {

    }

    private record Response(Map<String, TextEdit[]> textEdits) {

    }
}
