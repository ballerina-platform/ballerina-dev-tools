import ballerina/http;
import ballerina/log;

// Define the service
service /root/path\-id on new http:Listener(9090) {

    // Resource fucntions with escaped characters
    resource function get\#gre eting() returns string {
        return "Hello, World! Welcome to Ballerina Service";
    }

    resource function get gre\#eting() returns string {
        return "Hello, World! Welcome to Ballerina Service";
    }

    // Resource function to handle GET requests at the root path
    resource function post greeting(string name) returns string {
        return "Hello, " + name + "! Welcome to Ballerina Service";
    }

    // Resource function with path parameter
    resource function get echo/[string message]() returns string {
        return "Echo: " + message;
    }

    // Resource function to handle POST requests
    resource function post data(@http:Payload json payload) returns json {
        return {
            "message": "Data received successfully",
            "data": payload
        };
    }
}

public listener http:Listener securedEP = new (9091,
    secureSocket = {
        key: {
            certFile: "../resource/path/to/public.crt",
            keyFile: "../resource/path/to/private.key"
        }
    }
);

listener http:Listener refListener = securedEP;

final http:Client httpClient = check new ("");

@display {
    label: "BIT Service"
}
service / on securedEP, securedEP, new http:Listener(9092) {

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
}

