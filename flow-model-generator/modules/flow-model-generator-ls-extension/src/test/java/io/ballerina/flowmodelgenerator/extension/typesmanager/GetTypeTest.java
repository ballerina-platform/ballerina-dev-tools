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

package io.ballerina.flowmodelgenerator.extension.typesmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.GetTypeRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for retrieving types.
 *
 * @since 2.0.0
 */
public class GetTypeTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        GetTypeRequest request = new GetTypeRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString(),
                testConfig.position());
        JsonObject response = getResponse(request);
        JsonElement typeResponse = response.get("type");
        JsonElement refsResponse = response.get("refs");
        if (!typeResponse.equals(testConfig.type()) || !refsResponse.equals(testConfig.refs())) {
            TestConfig updateConfig = new TestConfig(testConfig.filePath(), testConfig.position(),
                    testConfig.description(), typeResponse, refsResponse);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(typeResponse, testConfig.type());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("get_record_type1.json")},
                {Path.of("get_record_type2.json")},
                {Path.of("get_record_type3.json")},
                {Path.of("get_record_type4.json")},
                {Path.of("get_record_type5.json")},
                {Path.of("get_union_type1.json")},
                {Path.of("get_service_class1.json")},
                {Path.of("get_service_class2.json")},
                {Path.of("get_service_class3.json")},
                {Path.of("get_enum_type1.json")},
                {Path.of("get_enum_type2.json")},
                {Path.of("get_array_type_def.json")},
                {Path.of("get_table_type_def.json")},
                {Path.of("get_error_type_def.json")},
        };
    }

    @Override
    protected String getResourceDir() {
        return "types_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetTypeTest.class;
    }

    @Override
    protected String getApiName() {
        return "getType";
    }

    @Override
    protected String getServiceName() {
        return "typesManager";
    }

    private record TestConfig(String filePath,
                              LinePosition position,
                              String description,
                              JsonElement type,
                              JsonElement refs) {
    }
}
