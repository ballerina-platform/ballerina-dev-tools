package io.ballerina.sequencemodelgenerator.ls.extension;

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

public class SequenceModelGeneratorTests {
    private static final Path RES_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String BALLERINA = "ballerina";
    private static final String RESPONSES = "responses";
    private static final String SEQUENCE_DESIGN_SERVICE = "sequenceModelGeneratorService/getSequenceDiagramModel";

    private Endpoint serviceEndpoint;

    @BeforeClass
    public void startLanguageServer() {
        this.serviceEndpoint = TestUtil.initializeLanguageSever();
    }



    @Test(description = "test sequence model with conditional statements")
    public void testSequenceModel1() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "test1.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(5, 0), LinePosition.from(15, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("test1Response.json"));

//        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
//        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
//        System.out.println(response.getSequenceDiagramModel().toString());
    }

    @Test(description = "test sequence model with comments")
    public void testSequenceModel2() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "test2.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(12, 0), LinePosition.from(24, 1));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("test2Response.json"));

//        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
//        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
//        System.out.println(response.getSequenceDiagramModel().toString());
    }


    @Test(description = "test sequence model connectors")
    public void testSequenceModel3() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(37, 4), LinePosition.from(56, 5));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest1.json"));

//        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
//        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
//        System.out.println(response.getSequenceDiagramModel().toString());
    }


    @Test(description = "test sequence model connector with possible client calls")
    public void testSequenceModel4() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(117, 4), LinePosition.from(133, 5));

        Assert.assertEquals(getFormattedResponse(request, serviceEndpoint),
                getExpectedResponse("connectorTest2.json"));

//        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
//        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
//        System.out.println(response.getSequenceDiagramModel().toString());
    }



    private String getExpectedResponse(String fileName) throws IOException {
        return Files.readString(RES_DIR.resolve(RESPONSES).resolve(Path.of(fileName)))
                .replaceAll("\\s+", "")
                .replaceAll("\\{srcPath}", RES_DIR.toString().replaceAll("\\\\", "/"));

    }

    private String getFormattedResponse(SequenceDiagramServiceRequest request, Endpoint serviceEndpoint)
            throws ExecutionException, InterruptedException {

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
        return response.getSequenceDiagramModel().toString().replaceAll("\\s+", "")
                .replaceAll("\\\\", "/");
    }




    // TODO : REMOVE


    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel6() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/int-oxi-allotment-ballerina-subscriber/src/oxi_allotment_ballerina_subscriber/subscriber.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(81, 0), LinePosition.from(126, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testSequenceModelNewModel7() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/int-oxi-allotment-ballerina-subscriber/src/oxi_allotment_ballerina_subscriber/subscriber_impl.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(7, 0), LinePosition.from(121, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunc8e")
    public void testSequenceModelNewHotelReservation() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/api-hotel-reservation-ballerina-svc/reservation-service/reservation_service.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(24, 4), LinePosition.from(34, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunc8e")
    public void testSequenceModelNewHotelReservation4() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/api-azure-storage-ballerina-sys/src/azure_storage_sys/service.bal");


        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(100, 4), LinePosition.from(112, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunc9e")
    public void testModelGeneratorForModuleVars() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve("ballerina/ballerina-internal-hr-code-main/api-hotel-reservation-ballerina-svc/reservation-service-mock/reservation_service.bal");
        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(21, 4), LinePosition.from(31, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testConnectors() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(104, 4), LinePosition.from(120, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testConnectors2() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "connectorTest.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(67, 4), LinePosition.from(82, 5));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }

    @Test(description = "test seqeunce")
    public void testConditionsWithoutInteractions() throws IOException, ExecutionException, InterruptedException {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("sequence_services", "ConditionalWithoutInteractions.bal"));

        SequenceDiagramServiceRequest request = new SequenceDiagramServiceRequest(projectPath.toString(),
                LinePosition.from(7, 0), LinePosition.from(21, 1));

        CompletableFuture<?> result = serviceEndpoint.request(SEQUENCE_DESIGN_SERVICE, request);
        SequenceDiagramServiceResponse response = (SequenceDiagramServiceResponse) result.get();
    }
}
