package io.ballerina.graphqlmodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GraphqlModelGeneratorServiceTests {
    private static final Path RES_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String BALLERINA = "ballerina";
    private static final String RESULTS = "results";
    private static final String PROJECT_DESIGN_SERVICE = "graphqlDesignService/getGraphqlModel";
    Gson gson = new GsonBuilder().serializeNulls().create();

    @Test(description = "test model generation for graphql")
    public void testMultiModuleProject() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_1.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(
                projectPath.toString(),LinePosition.from(3,1), LinePosition.from(10,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }

    @Test(description = "test model generation for graphql with services and records")
    public void testServiceAndRecords() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_2.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(3,1), LinePosition.from(25,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }

    @Test(description = "test model generation for graphql with input")
    public void testServiceWithInputType() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_3.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(24,1), LinePosition.from(29,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }

    @Test(description = "test model generation for graphql with interface")
    public void testServiceWithInterface() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_4.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(49,1), LinePosition.from(56,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }

    @Test(description = "test model generation for graphql with enums")
    public void testServiceWithEnums() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_5.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(16,1), LinePosition.from(21,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }

    @Test(description = "test model generation for graphql with resource subscription")
    public void testServiceWithSubscription() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_6.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(2,1), LinePosition.from(21,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }

    @Test(description = "test model generation for graphql with Hierarchical resource path")
    public void testServiceHierarchicalResourcePath() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("graphql_services", "graphqlService_7.bal"));

        Endpoint serviceEndpoint = TestUtil.initializeLanguageSever();
        TestUtil.openDocument(serviceEndpoint, projectPath);

        GraphqlDesignServiceRequest request = new GraphqlDesignServiceRequest(projectPath.toString(),
                LinePosition.from(8,1), LinePosition.from(26,1));

        CompletableFuture<?> result = serviceEndpoint.request(PROJECT_DESIGN_SERVICE, request);
        GraphqlDesignServiceResponse response = (GraphqlDesignServiceResponse) result.get();

    }


}

