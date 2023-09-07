import ballerina/http;
import ballerina/os;
import sequence_services.testModule;
import ballerinax/azure_storage_service.blobs as azureBlob;

final azureBlob:ConnectionConfig blobServiceConfig;
final azureBlob:BlobClient blobClient;
final azureBlob:ManagementClient blobMgtClient;

http:Client diseaseEpB5 = check new ("https://disease.sh/v3");

function init() returns error? {
    string accountName = os:getEnv("AZURE_BLOB_STORAGE_ACCOUNT_NAME");
    blobServiceConfig = {
            accessKeyOrSAS: os:getEnv("AZURE_BLOB_STORAGE_KEY"),
            accountName: accountName,
            authorizationMethod: "accessKey"
        };

    do {
            blobMgtClient = check new (blobServiceConfig);
            blobClient = check new (blobServiceConfig);
            testModule:hello();
        } on fail azureBlob:ServerError|azureBlob:ClientError err {
                EEE();
        }
}

service /v1 on new http:Listener(9000, {timeout: 300}) {

    // Endpoint for liveness, readiness, and startup probes for Kubernetes
    resource function get healthz(http:RequestContext ctx) 
            returns azureBlob:ListContainerResult {

        //@hide-in-sequence: variable initialization
        int a = 1;

        do {
            return check blobClient->listContainers(maxResults = 1);
        } on fail azureBlob:Error azureBlobError {
            return {nextMarker: "",  containerList: [], responseHeaders: {}};
        }
    }

   
}

isolated function handleAzureBlobError(http:RequestContext ctx, azureBlob:Error blobError) 
        returns error?|http:Response{

    if blobError is azureBlob:ServerError {
        
        azureBlob:ServerErrorDetail & readonly serverErrorDetail = blobError.detail();
        

        if blobError is azureBlob:BadRequestError {
            return <apiUtils:BadRequestError> {body: errorDetail};
        }

        if blobError is azureBlob:NotFoundError {
            return <apiUtils:NotFoundError> {body: errorDetail};
        }

        if blobError is azureBlob:ConflictError {
            return <apiUtils:ConflictError> {body: errorDetail};
        }

        return <apiUtils:InternalServerError> {body: errorDetail};
    } else {
        apiUtils:MessageCtx messageCtx = apiUtils:updateMessageCtx(requestContext = ctx, faultCode = errorCode, faultMessage = blobError.toString());
        apiUtils:logError(messageCtx = messageCtx);
        return <apiUtils:InternalServerError> {body: {code: errorCode, message: blobError.message(), detail: blobError.toString()}};
    }
}

function AAA() returns error? {
    http:Response res = check diseaseEpB5->/covid\-19/countries;
    check CCC();

}

function CCC() returns error?{
    http:Response re6s = check diseaseEpB5->/covid\-19/countries;
    check DDD();
}

function DDD() {
    http:Response res = check diseaseEpB5->/covid\-19/countries;
    check EEE();
}

function EEE() {
    if true {
        K();
    }
}

function K(){
    testModule:hello();
}