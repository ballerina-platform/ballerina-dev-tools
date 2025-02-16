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
import io.ballerina.flowmodelgenerator.extension.request.DataMapperSourceRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the generation of data mapper source.
 *
 * @since 2.0.0
 */
public class DataMappingSourceTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("variable1.json")},
                {Path.of("variable2.json")},
                {Path.of("variable3.json")},
                {Path.of("variable4.json")},
                {Path.of("variable5.json")},
//                {Path.of("variable6.json")},
                {Path.of("variable7.json")},
                {Path.of("variable8.json")},
                {Path.of("variable9.json")},
                {Path.of("variable10.json")},
                {Path.of("variable11.json")},
                {Path.of("variable12.json")},
                {Path.of("variable13.json")},
                {Path.of("variable14.json")},
                {Path.of("variable15.json")},
                {Path.of("variable16.json")},
                {Path.of("variable17.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        DataMapperSourceRequest request =
                new DataMapperSourceRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(),
                        testConfig.diagram(), testConfig.mappings(), "", testConfig.targetField());
        String source = getResponse(request).getAsJsonPrimitive("source").getAsString();

        if (!source.equals(testConfig.output())) {
            TestConfig updateConfig = new TestConfig(testConfig.source(), testConfig.description(),
                    testConfig.diagram(), testConfig.propertyKey(), testConfig.position(), testConfig.mappings(),
                    source, testConfig.targetField());
//            updateConfig(configJsonPath, updateConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "data_mapper_source";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DataMappingTypesTest.class;
    }

    @Override
    protected String getApiName() {
        return "getSource";
    }

    @Override
    protected String getServiceName() {
        return "dataMapper";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param source      The source file name
     * @param description The description of the test
     * @param diagram     The diagram to generate the source code
     * @param propertyKey The property key to generate the source code
     * @param position    The position to generate the source code
     * @param mappings    The expected data mapping model
     * @param output      generated source expression
     * @param targetField The target field to generate the source code
     */
    private record TestConfig(String source, String description, JsonElement diagram, String propertyKey,
                              JsonElement position, JsonArray mappings,
                              String output, String targetField) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
