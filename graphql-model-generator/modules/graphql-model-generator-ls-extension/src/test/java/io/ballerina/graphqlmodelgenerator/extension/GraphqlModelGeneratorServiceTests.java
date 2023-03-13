/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
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

package io.ballerina.graphqlmodelgenerator.extension;

import io.ballerina.tools.text.LinePosition;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Test cases for the graphql model generator service.
 *
 * @since 2201.5.0
 */
public class GraphqlModelGeneratorServiceTests {
    private static final Path RES_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String BALLERINA = "ballerina";
    private static final String RESPONSES = "responses";
    private static final String PROJECT_DESIGN_SERVICE = "graphqlDesignService/getGraphqlModel";

    private Endpoint serviceEndpoint;

    @BeforeClass
    public void startLanguageServer() {
        this.serviceEndpoint = TestUtil.initializeLanguageSever();
    }

    @Test(description = "test service with operations, outputs(enum,record,class), documentation and directives")
    public void testDifferentOutputsAndOperations() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "01_graphql_service.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(50, 0), LinePosition.from(89, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("01_graphql_service.json"));
    }


    @Test(description = "test service with input objects")
    public void testInputObjects() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "02_graphql_service.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(14, 0), LinePosition.from(25, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("02_graphql_service.json"));
    }

    @Test(description = "test service with interfaces")
    public void testServiceWithInterfaces() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "03_service_with_interfaces.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(52, 0), LinePosition.from(59, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("03_service_with_interfaces.json"));
    }

    @Test(description = "test service with union output")
    public void testServiceWithUnionOutput() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "04_service_with_union_outputs.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(48, 0), LinePosition.from(57, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("04_service_with_union_output.json"));
    }

    @Test(description = "test outputs from different files other than the service file")
    public void testObjectsFromDifferentFiles() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "05_outputs_from_different_file.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(4, 0), LinePosition.from(13, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("05_outputs_from_different_file.json"));
    }

    @Test(description = "test graphql file uploads")
    public void testFileUploads() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "06_file_uploads.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(13, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("06_file_uploads.json"));
    }

    @Test(description = "test resource paths with hierarchical paths")
    public void testHierarchicalResourcePaths() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "07_hierarchical_resource_paths.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(17, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("07_hierarchical_resource_paths.json"));
    }


    @Test(description = "test resource with invalid output")
    public void testResourceWithInvalidOutput() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "08_resource_with_invalid_return.bal"));

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(2, 0), LinePosition.from(7, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("08_resource_with_invalid_return.json"));
    }

    private String getExpectedResponse(String fileName) throws IOException {
        return Files.readString(RES_DIR.resolve(RESPONSES).resolve(Path.of(fileName)))
                .replaceAll("\\s+", "")
                .replaceAll("\\{srcPath}", RES_DIR.toString().replaceAll("\\\\", "/"));

    }

    private String getFormattedResponse(GraphqlDesignServiceRequest request, Endpoint serviceEndpoint)
            throws ExecutionException, InterruptedException {
        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();
        return response.getGraphqlDesignModel().toString().replaceAll("\\s+", "")
                .replaceAll("\\\\\\\\", "/");
    }
}
