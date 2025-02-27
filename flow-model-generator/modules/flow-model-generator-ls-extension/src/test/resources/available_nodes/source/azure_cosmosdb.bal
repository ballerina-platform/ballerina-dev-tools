import ballerina/log;
import ballerinax/azure_cosmosdb;

final azure_cosmosdb:DataPlaneClient moduleClient = check new ({baseUrl: "https://mycosmosaccount.documents.azure.com:443/", primaryKeyOrResourceToken: ""});

public function main() returns error? {
    azure_cosmosdb:DocumentResponse|error document = moduleClient->createDocument("id", "id", "docId", {}, "key");
    if document is azure_cosmosdb:DocumentResponse {
        log:printInfo("Document created successfully");
    } else {
        log:printError("Error creating document", 'error = document);
    }
}
