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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the abstract test class for the flow model generator service.
 *
 * @since 2201.9.0
 */
abstract class AbstractLSTest {

    protected static Logger LOG;
    protected static Path RES_DIR, SOURCE_DIR, CONFIG_DIR;
    protected final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;

    @BeforeClass
    public void init() {
        RES_DIR = Paths.get("src/test/resources").resolve(getResourceDir()).toAbsolutePath();
        CONFIG_DIR = RES_DIR.resolve("config");
        SOURCE_DIR = RES_DIR.resolve("source");
        LOG = LoggerFactory.getLogger(clazz());
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer().withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
    }

    @Test(dataProvider = "data-provider")
    public abstract void test(Path config) throws IOException;

    @DataProvider(name = "data-provider")
    protected Object[] getConfigsList() {
        List<String> skippedTests = Arrays.stream(this.skipList()).toList();
        try {
            return Files.walk(RES_DIR)
                    .filter(path -> {
                        File file = path.toFile();
                        return file.isFile() && file.getName().endsWith(".json")
                                && !skippedTests.contains(file.getName());
                    })
                    .toArray();
        } catch (IOException e) {
            // If failed to load tests, then it's a failure
            Assert.fail("Unable to load test config", e);
            return new Object[0][];
        }
    }

    protected String[] skipList() {
        return new String[]{};
    }

    protected void updateConfig(Path configJsonPath, Object updatedConfig) throws IOException {
        String objStr = gson.toJson(updatedConfig).concat(System.lineSeparator());
        Files.writeString(configJsonPath, objStr);
    }

    protected String getResponse(Object request) throws IOException {
        CompletableFuture<?> result = this.serviceEndpoint.request("flowDesignService/" + getServiceName(), request);
        return TestUtil.getResponseString(result);
    }

    protected abstract String getResourceDir();

    protected abstract Class<? extends AbstractLSTest> clazz();

    protected abstract String getServiceName();

    @AfterClass
    public void shutDownLanguageServer() {
        TestUtil.shutdownLanguageServer(this.serviceEndpoint);
        this.languageServer = null;
        this.serviceEndpoint = null;
    }
}
