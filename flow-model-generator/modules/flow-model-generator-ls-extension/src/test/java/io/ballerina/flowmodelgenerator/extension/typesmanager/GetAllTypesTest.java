package io.ballerina.flowmodelgenerator.extension.typesmanager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.extension.AbstractLSTest;
import io.ballerina.flowmodelgenerator.extension.request.FilePathRequest;
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
public class GetAllTypesTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        FilePathRequest request = new FilePathRequest(
                sourceDir.resolve(testConfig.filePath()).toAbsolutePath().toString());
        JsonArray response = getResponse(request).getAsJsonArray("types");
        if (!response.equals(testConfig.types())) {
            TestConfig updateConfig = new TestConfig(testConfig.filePath(), testConfig.description(), response);
//            updateConfig(configJsonPath, updateConfig);
            compareJsonElements(response, testConfig.types());
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("get_all_types.json")},
        };
    }

    @Override
    protected String getResourceDir() {
        return "types_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetAllTypesTest.class;
    }

    @Override
    protected String getApiName() {
        return "getTypes";
    }

    @Override
    protected String getServiceName() {
        return "typesManager";
    }

    private record TestConfig(String filePath, String description, JsonElement types) {
    }
}
