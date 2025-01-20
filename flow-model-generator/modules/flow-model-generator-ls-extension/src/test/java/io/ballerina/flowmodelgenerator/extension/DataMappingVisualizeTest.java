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
import io.ballerina.flowmodelgenerator.extension.request.DataMapperVisualizeRequest;
import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the visualization of data mapper model.
 *
 * @since 2.0.0
 */
public class DataMappingVisualizeTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("variable1.json")},
                {Path.of("variable2.json")},
                {Path.of("variable3.json")},
                {Path.of("variable4.json")},
                {Path.of("variable5.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Endpoint endpoint = TestUtil.newLanguageServer().withLanguageServer(new BallerinaLanguageServer()).build();
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        DataMapperVisualizeRequest request =
                new DataMapperVisualizeRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(),
                        testConfig.diagram(), testConfig.position());
        JsonArray model = getResponse(endpoint, request).getAsJsonArray("visualizableProperties");

        if (!model.equals(testConfig.visualizableProperties())) {
            TestConfig updateConfig = new TestConfig(testConfig.source(), testConfig.description(),
                    testConfig.diagram(), testConfig.position(), model);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(model, testConfig.visualizableProperties());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "data_mapper_visualize";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DataMappingTypesTest.class;
    }

    @Override
    protected String getApiName() {
        return "visualizable";
    }

    @Override
    protected String getServiceName() {
        return "dataMapper";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param source                 The source file name
     * @param description            The description of the test
     * @param diagram                The diagram to generate the source code
     * @param position               The position to get the visualizable properties
     * @param visualizableProperties The expected visualizable properties
     */
    private record TestConfig(String source, String description, JsonElement diagram, LinePosition position,
                              JsonElement visualizableProperties) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
