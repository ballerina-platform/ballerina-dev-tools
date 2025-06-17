import imported_listener.mod1;

import ballerina/http;
import ballerina/log;
import ballerina/soap;

listener http:Listener refListener = mod1:securedEP;

listener http:Listener securedEP = new (9090,
    secureSocket = {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
);

final http:Client httpClient = check new ("");

@display {
    label: "BIT Service"
}
service / on securedEP, mod1:securedEP, new http:Listener(9092) {

    function init() returns error? {
    }

    resource function get greeting() returns json|http:InternalServerError {
        do {
            json j = check httpClient->/;
            log:printInfo(j.toJsonString());
        } on fail error e {
            log:printError("Error: ", 'error = e);
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}

service /api/v1 on refListener {
    
    resource function get path() returns json|http:InternalServerError {
        do {
            return 0;
        } on fail error e {
            log:printError("Error: ", 'error = e);
            return http:INTERNAL_SERVER_ERROR;
        }
    }

    resource function get path2() returns json|http:InternalServerError {
        do {
            worker processExpenditureTime {
                do {
                    http:Client keyVaultClient = check new ("http://localhost:9090/keyVault");
                    json keyVaultResponse = check keyVaultClient->get("gSolutionP");
                } on fail error e {
                    // Update instance status as failed
                }
            }
        } on fail error e {
            log:printError("Error: ", 'error = e);
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}
