package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperModelRequest;
import io.ballerina.flowmodelgenerator.extension.request.DataMapperSourceRequest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataMappingSourceTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
//                {Path.of("variable1.json")},
                {Path.of("variable2.json")},
//                {Path.of("variable3.json")},
//                {Path.of("variable4.json")},
//                {Path.of("variable5.json")},
//                {Path.of("variable6.json")},
//                {Path.of("variable7.json")},
//                {Path.of("variable8.json")},
//                {Path.of("variable9.json")},
//                {Path.of("variable10.json")},
//                {Path.of("variable11.json")},
//                {Path.of("variable12.json")},
//                {Path.of("variable13.json")},
//                {Path.of("variable14.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        DataMapperSourceRequest request =
                new DataMapperSourceRequest(sourceDir.resolve(testConfig.source()).toAbsolutePath().toString(),
                        testConfig.diagram(), testConfig.mappings());
        JsonObject model = getResponse(request).getAsJsonObject("getSource");

//        if (!model.equals(testConfig.model())) {
//            TestConfig updateConfig = new TestConfig(testConfig.source(), testConfig.description(),
//                    testConfig.diagram(), testConfig.propertyKey(), testConfig.position(), model);
////            updateConfig(configJsonPath, updateConfig);
//            compareJsonElements(model, testConfig.model());
//            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
//        }
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
     * @param mappings       The expected data mapping model
     */
    private record TestConfig(String source, String description, JsonElement diagram, JsonArray mappings) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
