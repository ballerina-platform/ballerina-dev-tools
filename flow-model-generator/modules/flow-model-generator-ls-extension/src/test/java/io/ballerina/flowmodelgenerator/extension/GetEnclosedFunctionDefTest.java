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
import com.google.gson.JsonPrimitive;
import io.ballerina.flowmodelgenerator.extension.request.EnclosedFuncDefRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the flow model source generator service.
 *
 * @since 2.0.0
 */
public class GetEnclosedFunctionDefTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String filePath = getSourcePath(testConfig.filePath());
        EnclosedFuncDefRequest request = new EnclosedFuncDefRequest(filePath, testConfig.position());
        JsonObject response = getResponse(request);
        JsonObject acutalJsonObj = testConfig.response();
        acutalJsonObj.add("filePath", new JsonPrimitive(
                getSourcePath(testConfig.response().get("filePath").getAsString())));
        if (!testConfig.response().equals(response)) {
            String pathToReplace = response.get("filePath").getAsString()
                    .replace(sourceDir.toString() + "/", "");
            response.addProperty("filePath", pathToReplace);
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.filePath(),
                    testConfig.position(), response);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "get_enclosed_func_def";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetEnclosedFunctionDefTest.class;
    }

    @Override
    protected String getApiName() {
        return "getEnclosedFunctionDef";
    }

    /**
     * Represents the test configuration for the delete node test.
     *
     * @param description The description of the test.
     * @param filePath    The path to the source file that contains the nodes to be deleted.
     * @param position    The position of the node to be deleted.
     * @param response    The expected response.
     */
    private record TestConfig(String description, String filePath, LinePosition position, JsonObject response) {
    }
}
