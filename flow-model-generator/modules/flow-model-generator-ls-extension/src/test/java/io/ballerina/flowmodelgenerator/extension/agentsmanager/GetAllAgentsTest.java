package io.ballerina.flowmodelgenerator.extension.agentsmanager;

import com.google.gson.JsonArray;
import io.ballerina.flowmodelgenerator.extension.request.GetAllAgentsRequest;
import io.ballerina.modelgenerator.commons.AbstractLSTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetAllAgentsTest extends AbstractLSTest {

    @DataProvider(name = "data-provider")
    @Override
    protected Object[] getConfigsList() {
        return new Object[][]{
                {Path.of("get_all_agents.json")}
        };
    }

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = configDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        String filePath =
                testConfig.source() == null ? "" : sourceDir.resolve(testConfig.source()).toAbsolutePath().toString();
        GetAllAgentsRequest request = new GetAllAgentsRequest(filePath);
        JsonArray agents = getResponse(request).getAsJsonArray("agents");

        if (!agents.equals(testConfig.agents())) {
            TestConfig updatedConfig = new TestConfig(testConfig.source(), testConfig.description(), agents);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail("Test failed. Updated the expected output in " + configJsonPath);
        }
    }

    @Override
    protected String getResourceDir() {
        return "agents_manager";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return GetAllAgentsTest.class;
    }

    @Override
    protected String getApiName() {
        return "getAllAgents";
    }

    @Override
    protected String getServiceName() {
        return "agentManager";
    }

    /**
     * Represents the test configuration for the flow model getNodeTemplate API.
     *
     * @param source      The source file path
     * @param description The description of the test
     * @param agents      List of all available agents
     */
    private record TestConfig(String source, String description, JsonArray agents) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
