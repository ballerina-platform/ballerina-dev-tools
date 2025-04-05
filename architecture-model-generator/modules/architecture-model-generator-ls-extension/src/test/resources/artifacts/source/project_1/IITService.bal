import ballerina/http;
import ballerina/log;

service / on new http:Listener(9091) {

    function init() returns error? {
    }

    resource function get greeting() returns json|http:InternalServerError {
        do {
            check foo();
        } on fail error e {
            log:printError("Error: ", 'error = e);
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}

function foo() returns error? {
    json j = check httpClient2->/;
    log:printInfo(j.toJsonString());
}
