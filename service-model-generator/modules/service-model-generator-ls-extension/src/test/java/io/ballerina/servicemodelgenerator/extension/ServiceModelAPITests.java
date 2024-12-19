package io.ballerina.servicemodelgenerator.extension;

import io.ballerina.servicemodelgenerator.extension.model.Codedata;
import io.ballerina.servicemodelgenerator.extension.model.Function;
import io.ballerina.servicemodelgenerator.extension.model.Listener;
import io.ballerina.servicemodelgenerator.extension.model.Parameter;
import io.ballerina.servicemodelgenerator.extension.model.Service;
import io.ballerina.servicemodelgenerator.extension.model.Value;
import io.ballerina.servicemodelgenerator.extension.request.CommonModelFromSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerDiscoveryRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.ListenerSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ResourceSourceRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceModelRequest;
import io.ballerina.servicemodelgenerator.extension.request.ServiceSourceRequest;
import io.ballerina.servicemodelgenerator.extension.response.CommonSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerDiscoveryResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerFromSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ListenerModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ResourceModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceFromSourceResponse;
import io.ballerina.servicemodelgenerator.extension.response.ServiceModelResponse;
import io.ballerina.servicemodelgenerator.extension.response.TriggerListResponse;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.ballerinalang.langserver.BallerinaLanguageServer;
import org.ballerinalang.langserver.util.TestUtil;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ServiceModelAPITests {

    private Endpoint serviceEndpoint;
    private BallerinaLanguageServer languageServer;
    private Path resDir;

    @BeforeClass
    public void init() {
        this.languageServer = new BallerinaLanguageServer();
        TestUtil.LanguageServerBuilder builder = TestUtil.newLanguageServer().withLanguageServer(languageServer);
        this.serviceEndpoint = builder.build();
        resDir = Paths.get("src/test/resources/samples").toAbsolutePath();
    }

    @Test
    public void testListenerDiscoveryWithEmptyFile() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample1/main.bal");
        ListenerDiscoveryRequest request = new ListenerDiscoveryRequest(filePath.toAbsolutePath().toString(),
                "ballerina", "http");
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getListeners", request);
        ListenerDiscoveryResponse response = (ListenerDiscoveryResponse) result.get();
        Assert.assertFalse(response.hasListeners());
        Assert.assertEquals(response.listeners().size(), 0);
    }

    @Test
    public void testListenerDiscovery() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample2/main.bal");
        ListenerDiscoveryRequest request = new ListenerDiscoveryRequest(filePath.toAbsolutePath().toString(),
                "ballerinax", "nats");
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getListeners", request);
        ListenerDiscoveryResponse response = (ListenerDiscoveryResponse) result.get();
        Assert.assertFalse(response.hasListeners());
        Assert.assertEquals(response.listeners().size(), 0);

        request = new ListenerDiscoveryRequest(filePath.toAbsolutePath().toString(),
                "ballerinax", "kafka");
        result = serviceEndpoint.request("serviceDesign/getListeners", request);
        response = (ListenerDiscoveryResponse) result.get();
        Assert.assertTrue(response.hasListeners());
        Assert.assertEquals(response.listeners().size(), 1);
        Assert.assertTrue(response.listeners().contains("kafkaListener"));

        request = new ListenerDiscoveryRequest(filePath.toAbsolutePath().toString(),
                "ballerinax", "http");
        result = serviceEndpoint.request("serviceDesign/getListeners", request);
        response = (ListenerDiscoveryResponse) result.get();
        Assert.assertTrue(response.hasListeners());
        Assert.assertEquals(response.listeners().size(), 2);
        Assert.assertTrue(response.listeners().contains("httpListener"));
        Assert.assertTrue(response.listeners().contains("githubListener"));

        request = new ListenerDiscoveryRequest(filePath.toAbsolutePath().toString(),
                "ballerinax", "kafka");
        result = serviceEndpoint.request("serviceDesign/getListeners", request);
        response = (ListenerDiscoveryResponse) result.get();
        Assert.assertTrue(response.hasListeners());
        Assert.assertEquals(response.listeners().size(), 1);
        Assert.assertTrue(response.listeners().contains("kafkaListener"));
    }

    @Test
    public void testGetHttpListenerModel() throws ExecutionException, InterruptedException {
        ListenerModelRequest request = new ListenerModelRequest("ballerina", "http");
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getListenerModel", request);
        ListenerModelResponse response = (ListenerModelResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.listener()));
    }

    @Test
    public void testAddHttpListener() throws ExecutionException, InterruptedException {
        ListenerModelRequest modelRequest = new ListenerModelRequest("ballerina", "http");
        CompletableFuture<?> modelResult = serviceEndpoint.request("serviceDesign/getListenerModel", modelRequest);
        ListenerModelResponse modelResponse = (ListenerModelResponse) modelResult.get();
        Listener listener = modelResponse.listener();

        Value name = listener.getProperty("name");
        name.setValue("httpListener");
        Value port = listener.getProperty("port");
        port.setValue("9999");
        Value httpVersion = listener.getProperty("httpVersion");
        httpVersion.setValue("\"1.1\"");
        httpVersion.setEnabled(true);

        Path filePath = resDir.resolve("sample2/main.bal");
        ListenerSourceRequest sourceRequest = new ListenerSourceRequest(filePath.toAbsolutePath().toString(),
                listener);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/addListener", sourceRequest);
        CommonSourceResponse sourceResponse = (CommonSourceResponse) sourceResult.get();
        Assert.assertTrue(Objects.nonNull(sourceResponse.textEdits()));
        Assert.assertFalse(sourceResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetServiceModelWithoutListener() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample1/main.bal");
        ServiceModelRequest request = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerina", 
                "http", null);
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getServiceModel", request);
        ServiceModelResponse response = (ServiceModelResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.service()));

        filePath = resDir.resolve("sample2/main.bal");
        request = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerina",
                "http", null);
        result = serviceEndpoint.request("serviceDesign/getServiceModel", request);
        response = (ServiceModelResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.service()));

        filePath = resDir.resolve("sample2/main.bal");
        request = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerinax",
                "kafka", null);
        result = serviceEndpoint.request("serviceDesign/getServiceModel", request);
        response = (ServiceModelResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.service()));
    }

    @Test
    public void testGetServiceModelWithListener() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample2/main.bal");
        ServiceModelRequest request = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerinax",
                "rabbitmq", "testListener");
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getServiceModel", request);
        ServiceModelResponse response = (ServiceModelResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.service()));
    }

    @Test
    public void testGetHttpResourceModel() throws ExecutionException, InterruptedException {
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getHttpResourceModel", null);
        ResourceModelResponse response = (ResourceModelResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.resource()));
    }

    @Test
    public void testGetTriggerList() throws ExecutionException, InterruptedException {
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getTriggerModels", null);
        TriggerListResponse response = (TriggerListResponse) result.get();
        Assert.assertTrue(Objects.nonNull(response.local()));
        Assert.assertFalse(response.local().isEmpty());
    }

    @Test
    public void testAddHttpService() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample2/main.bal");
        ServiceModelRequest modelRequest = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerina",
                "http", null);
        CompletableFuture<?> modelResult = serviceEndpoint.request("serviceDesign/getServiceModel", modelRequest);
        ServiceModelResponse modelResponse = (ServiceModelResponse) modelResult.get();
        Service service = modelResponse.service();
        Assert.assertTrue(Objects.nonNull(service));
        service.getListener().setValues(List.of("httpTestListener", "httpsTestListener"));
        Value designApproach = service.getDesignApproach();
        Value selectedApproach = designApproach.getChoices().get(0);
        Value basePath = selectedApproach.getProperty("basePath");
        basePath.setValue("/api/test");
        basePath.setEnabled(true);
        selectedApproach.setEnabled(true);

        ServiceSourceRequest sourceRequest = new ServiceSourceRequest(filePath.toAbsolutePath().toString(), service);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/addService", sourceRequest);
        CommonSourceResponse sourceResponse = (CommonSourceResponse) sourceResult.get();
        Assert.assertTrue(Objects.nonNull(sourceResponse.textEdits()));
        Assert.assertFalse(sourceResponse.textEdits().isEmpty());
    }

    @Test
    public void testAddHttpServiceWithContract() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample5/main.bal");
        ServiceModelRequest modelRequest = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerina",
                "http", null);
        CompletableFuture<?> modelResult = serviceEndpoint.request("serviceDesign/getServiceModel", modelRequest);
        ServiceModelResponse modelResponse = (ServiceModelResponse) modelResult.get();
        Service service = modelResponse.service();
        Assert.assertTrue(Objects.nonNull(service));
        service.getListener().setValues(List.of("httpListener", "httpsListener"));
        Value designApproach = service.getDesignApproach();
        designApproach.getChoices().getFirst().setEnabled(false);
        Value selectedApproach = designApproach.getChoices().get(1);
        Value serviceTypeName = selectedApproach.getProperty("serviceTypeName");
        serviceTypeName.setValue("AlbumsService");
        serviceTypeName.setEnabled(true);
        Value contractPath = selectedApproach.getProperty("spec");
        contractPath.setValue(resDir.resolve("sample5/openapi.yaml").toAbsolutePath().toString());
        contractPath.setEnabled(true);
        selectedApproach.setEnabled(true);

        ServiceSourceRequest sourceRequest = new ServiceSourceRequest(filePath.toAbsolutePath().toString(), service);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/addService", sourceRequest);
        CommonSourceResponse sourceResponse = (CommonSourceResponse) sourceResult.get();
        Assert.assertTrue(Objects.nonNull(sourceResponse.textEdits()));
        Assert.assertFalse(sourceResponse.textEdits().isEmpty());
    }
    
    @Test
    public void testAddHttpResource() throws ExecutionException, InterruptedException {
        CompletableFuture<?> modelResult = serviceEndpoint.request("serviceDesign/getHttpResourceModel", null);
        ResourceModelResponse modelResponse = (ResourceModelResponse) modelResult.get();

        Function resource = modelResponse.resource();
        resource.getAccessor().setValue("POST");
        resource.getName().setValue("test/[string name]/api");

        Parameter query = resource.getSchema().get("query");
        query.setEnabled(true);
        query.getName().setValue("id");
        query.getType().setValue("int");
        resource.addParameter(query);

        Parameter payload = resource.getSchema().get("payload");
        payload.setEnabled(true);
        payload.getName().setValue("body");
        payload.getType().setValue("map<json>");
        resource.addParameter(payload);

        Parameter header = resource.getSchema().get("header");
        header.setEnabled(true);
        header.getName().setValue("header");
        header.getType().setValue("string");
        resource.addParameter(header);

        Path filePath = resDir.resolve("sample3/main.bal");
        Codedata codedata = new Codedata(LineRange.from("main.bal", LinePosition.from(7, 0),
                LinePosition.from(21, 1)));
        ResourceSourceRequest sourceRequest = new ResourceSourceRequest(filePath.toAbsolutePath().toString(),
                resource, codedata);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/addResource", sourceRequest);
        CommonSourceResponse sourceResponse = (CommonSourceResponse) sourceResult.get();
        Assert.assertTrue(Objects.nonNull(sourceResponse.textEdits()));
        Assert.assertFalse(sourceResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetHttpServiceFromSource() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample3/main.bal");
        Codedata codedata = new Codedata(LineRange.from("main.bal", LinePosition.from(7, 0),
                LinePosition.from(21, 1)));
        CommonModelFromSourceRequest sourceRequest = new CommonModelFromSourceRequest(
                filePath.toAbsolutePath().toString(), codedata);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/getServiceFromSource",
                sourceRequest);
        ServiceFromSourceResponse sourceResponse = (ServiceFromSourceResponse) sourceResult.get();
        Service service = sourceResponse.service();
        Assert.assertTrue(Objects.nonNull(service));

        ServiceSourceRequest genRequest = new ServiceSourceRequest(filePath.toAbsolutePath().toString(), service);
        CompletableFuture<?> genResult = serviceEndpoint.request("serviceDesign/addService", genRequest);
        CommonSourceResponse genResponse = (CommonSourceResponse) genResult.get();
        Assert.assertTrue(Objects.nonNull(genResponse.textEdits()));
        Assert.assertFalse(genResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetHttpContractServiceFromSource() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample6/main.bal");
        Codedata codedata = new Codedata(LineRange.from("main.bal", LinePosition.from(7, 0),
                LinePosition.from(21, 1)));
        CommonModelFromSourceRequest sourceRequest = new CommonModelFromSourceRequest(
                filePath.toAbsolutePath().toString(), codedata);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/getServiceFromSource",
                sourceRequest);
        ServiceFromSourceResponse sourceResponse = (ServiceFromSourceResponse) sourceResult.get();
        Service service = sourceResponse.service();
        Assert.assertTrue(Objects.nonNull(service));

        ServiceSourceRequest genRequest = new ServiceSourceRequest(filePath.toAbsolutePath().toString(), service);
        CompletableFuture<?> genResult = serviceEndpoint.request("serviceDesign/addService", genRequest);
        CommonSourceResponse genResponse = (CommonSourceResponse) genResult.get();
        Assert.assertTrue(Objects.nonNull(genResponse.textEdits()));
        Assert.assertFalse(genResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetHttpListenerFromSource() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample3/main.bal");
        Codedata codedata = new Codedata(LineRange.from("main.bal", LinePosition.from(5, 0),
                LinePosition.from(5, 56)));
        CommonModelFromSourceRequest sourceRequest = new CommonModelFromSourceRequest(
                filePath.toAbsolutePath().toString(), codedata);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/getListenerFromSource",
                sourceRequest);
        ListenerFromSourceResponse sourceResponse = (ListenerFromSourceResponse) sourceResult.get();
        Listener listener = sourceResponse.listener();
        Assert.assertTrue(Objects.nonNull(listener));

        ListenerSourceRequest genRequest = new ListenerSourceRequest(filePath.toAbsolutePath().toString(), listener);
        CompletableFuture<?> genResult = serviceEndpoint.request("serviceDesign/addListener", genRequest);
        CommonSourceResponse genResponse = (CommonSourceResponse) genResult.get();
        Assert.assertTrue(Objects.nonNull(genResponse.textEdits()));
        Assert.assertFalse(genResponse.textEdits().isEmpty());
    }

    @Test
    public void testAddTriggerListener() throws ExecutionException, InterruptedException {
        ListenerModelRequest modelRequest = new ListenerModelRequest("ballerinax", "trigger.github");
        CompletableFuture<?> modelResult = serviceEndpoint.request("serviceDesign/getListenerModel", modelRequest);
        ListenerModelResponse modelResponse = (ListenerModelResponse) modelResult.get();
        Listener listener = modelResponse.listener();
        Assert.assertTrue(Objects.nonNull(listener));

        Value name = listener.getProperty("name");
        name.setValue("githubTestListener");
        Value listenerConfig = listener.getProperty("listenerConfig");
        listenerConfig.setValue("{webhookSecret: \"secret\"}");
        Value port = listener.getProperty("listenOn");
        port.setValue("9119");
        port.setEnabled(true);

        Path filePath = resDir.resolve("sample4/main.bal");
        ListenerSourceRequest sourceRequest = new ListenerSourceRequest(filePath.toAbsolutePath().toString(),
                listener);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/addListener", sourceRequest);
        CommonSourceResponse sourceResponse = (CommonSourceResponse) sourceResult.get();
        Assert.assertTrue(Objects.nonNull(sourceResponse.textEdits()));
        Assert.assertFalse(sourceResponse.textEdits().isEmpty());
    }

    @Test
    public void testAddTriggerService() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample2/main.bal");
        ServiceModelRequest modelRequest = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerinax",
                "trigger.github", null);
        CompletableFuture<?> modelResult = serviceEndpoint.request("serviceDesign/getServiceModel", modelRequest);
        ServiceModelResponse modelResponse = (ServiceModelResponse) modelResult.get();
        Service service = modelResponse.service();
        Assert.assertTrue(Objects.nonNull(service));
        service.getListener().setValue("githubTestListener");
        Value serviceTypeValue = service.getServiceType();
        List<String> serviceTypes = serviceTypeValue.getItems();
        serviceTypeValue.setValue(serviceTypes.get(9));

        ServiceSourceRequest sourceRequest = new ServiceSourceRequest(filePath.toAbsolutePath().toString(), service);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/addService", sourceRequest);
        CommonSourceResponse sourceResponse = (CommonSourceResponse) sourceResult.get();
        Assert.assertTrue(Objects.nonNull(sourceResponse.textEdits()));
        Assert.assertFalse(sourceResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetTriggerServiceFromSource() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample4/main.bal");
        Codedata codedata = new Codedata(LineRange.from("main.bal", LinePosition.from(4, 0),
                LinePosition.from(53, 1)));
        CommonModelFromSourceRequest sourceRequest = new CommonModelFromSourceRequest(
                filePath.toAbsolutePath().toString(), codedata);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/getServiceFromSource",
                sourceRequest);
        ServiceFromSourceResponse sourceResponse = (ServiceFromSourceResponse) sourceResult.get();
        Service service = sourceResponse.service();
        Assert.assertTrue(Objects.nonNull(service));

        ServiceSourceRequest genRequest = new ServiceSourceRequest(filePath.toAbsolutePath().toString(), service);
        CompletableFuture<?> genResult = serviceEndpoint.request("serviceDesign/addService", genRequest);
        CommonSourceResponse genResponse = (CommonSourceResponse) genResult.get();
        Assert.assertTrue(Objects.nonNull(genResponse.textEdits()));
        Assert.assertFalse(genResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetTriggerListenerFromSource() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample4/main.bal");
        Codedata codedata = new Codedata(LineRange.from("main.bal", LinePosition.from(2, 0),
                LinePosition.from(2, 112)));
        CommonModelFromSourceRequest sourceRequest = new CommonModelFromSourceRequest(
                filePath.toAbsolutePath().toString(), codedata);
        CompletableFuture<?> sourceResult = serviceEndpoint.request("serviceDesign/getListenerFromSource",
                sourceRequest);
        ListenerFromSourceResponse sourceResponse = (ListenerFromSourceResponse) sourceResult.get();
        Listener listener = sourceResponse.listener();
        Assert.assertTrue(Objects.nonNull(listener));

        ListenerSourceRequest genRequest = new ListenerSourceRequest(filePath.toAbsolutePath().toString(), listener);
        CompletableFuture<?> genResult = serviceEndpoint.request("serviceDesign/addListener", genRequest);
        CommonSourceResponse genResponse = (CommonSourceResponse) genResult.get();
        Assert.assertTrue(Objects.nonNull(genResponse.textEdits()));
        Assert.assertFalse(genResponse.textEdits().isEmpty());
    }

    @Test
    public void testGetTriggerServiceModelWithExistingServices() throws ExecutionException, InterruptedException {
        Path filePath = resDir.resolve("sample4/main.bal");
        ServiceModelRequest request = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerinax",
                "trigger.github", null);
        CompletableFuture<?> result = serviceEndpoint.request("serviceDesign/getServiceModel", request);
        ServiceModelResponse response = (ServiceModelResponse) result.get();
        Service service = response.service();
        Assert.assertTrue(Objects.nonNull(response.service()));
        Value serviceType = service.getServiceType();
        Assert.assertEquals(serviceType.getItems().size(), 10);

        request = new ServiceModelRequest(filePath.toAbsolutePath().toString(), "ballerinax",
                "trigger.github", "githubTestListener");
        result = serviceEndpoint.request("serviceDesign/getServiceModel", request);
        response = (ServiceModelResponse) result.get();
        service = response.service();
        Assert.assertTrue(Objects.nonNull(response.service()));
        serviceType = service.getServiceType();
        Assert.assertEquals(serviceType.getItems().size(), 8);
    }
}