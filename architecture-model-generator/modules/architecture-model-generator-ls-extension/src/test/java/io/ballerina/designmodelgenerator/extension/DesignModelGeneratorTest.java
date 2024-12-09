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


package io.ballerina.designmodelgenerator.extension;

import com.google.gson.JsonObject;
import io.ballerina.desginmodelgenerator.extension.request.GetDesignModelRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for getting the design model for a package.
 *
 * @since 2.0.0
 */
public class DesignModelGeneratorTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String sourceFile = sourceDir.resolve(testConfig.projectPath()).toAbsolutePath().toString();
        GetDesignModelRequest request = new GetDesignModelRequest(sourceFile);
        JsonObject jsonObject = getResponse(request);
        Assert.assertEquals(jsonObject, testConfig.output());
    }

    @Override
    protected String getResourceDir() {
        return "get_design_model";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return DesignModelGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "getDesignModel";
    }

    public record TestConfig(String description, String projectPath, JsonObject output) {

    }
}
