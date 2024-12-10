package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonObject;
import io.ballerina.flowmodelgenerator.extension.request.OpenAPIClientGenerationRequest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientGeneratorTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("config1.json")},
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        Path contractPath = resDir.resolve("contracts").resolve(testConfig.contractFile());

        Path project = configDir.resolve(config.getFileName().toString().split(".json")[0]);
        Files.createDirectories(project);
        Path balToml = project.resolve("Ballerina.toml");
        Files.createFile(balToml);
        Files.writeString(balToml, testConfig.balToml());
        String projectPath = project.toAbsolutePath().toString();
        OpenAPIClientGenerationRequest req = new OpenAPIClientGenerationRequest(contractPath.toAbsolutePath().toString(), projectPath, testConfig.module());
        JsonObject resp = getResponse(req);
        deleteFolder(project.toFile());
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
    }

    @Override
    protected String getResourceDir() {
        return "openapi_client_gen";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ClientGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "genClient";
    }

    @Override
    protected String getServiceName() {
        return "openAPIService";
    }

    /**
     * Represents the test configuration for the service generation.
     *
     * @param contractFile OpenAPI contract file
     * @param lineRange    line range of service declaration
     * @since 1.4.0
     */
    private record TestConfig(String contractFile, String balToml, JsonObject lineRange, String module) {

    }
}
