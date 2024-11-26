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
import io.ballerina.flowmodelgenerator.extension.request.CopilotContextRequest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the copilot context service.
 *
 * @since 1.4.0
 */
public class CopilotContextTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        CopilotContextRequest request =
                new CopilotContextRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(),
                        testConfig.position());
        JsonObject response = getResponse(request);
        String prefix = response.get("prefix").getAsString();
        String suffix = response.get("suffix").getAsString();

        if (!prefix.equals(testConfig.prefix()) || !suffix.equals(testConfig.suffix())) {
            TestConfig updateConfig = new TestConfig(testConfig.description(), testConfig.position(),
                    testConfig.source(), prefix, suffix);
//            updateConfig(config, updateConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "copilot_context";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return CopilotContextTest.class;
    }

    @Override
    protected String getApiName() {
        return "getCopilotContext";
    }

    /**
     * Represents the test configuration for the available nodes test.
     *
     * @param description The description of the test
     * @param position    The position of the node to be added
     * @param source      The source file path
     * @param prefix      The prefix of the source to be sent
     * @param suffix      The suffix of the source to be sent
     */
    private record TestConfig(String description, LinePosition position, String source, String prefix, String suffix) {

    }
}
