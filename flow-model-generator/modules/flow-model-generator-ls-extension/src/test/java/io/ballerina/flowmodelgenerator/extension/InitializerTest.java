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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.extension.request.CreateFilesRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
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
public class InitializerTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        CreateFilesRequest request =
                new CreateFilesRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString());
        JsonArray filesCreated = getResponse(request).getAsJsonArray("files");

        for (JsonElement file : filesCreated) {
            Files.delete(sourceDir.resolve(testConfig.source()).resolve(file.getAsString()));
        }

        if (!filesCreated.equals(testConfig.files())) {
            TestConfig updatedConfig =
                    new TestConfig(testConfig.source(), testConfig.description(), filesCreated);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "initializer";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return InitializerTest.class;
    }

    @Override
    protected String getApiName() {
        return "createFiles";
    }

    @Override
    protected String getServiceName() {
        return "initializer";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param source      The source file name
     * @param description The description of the test
     * @param files       The file names that are created
     */
    private record TestConfig(String source, String description, JsonArray files) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
