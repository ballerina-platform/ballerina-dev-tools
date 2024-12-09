import ballerina/http;
import ballerina/log;

service / on new http:Listener(9092) {

    function init() returns error? {}

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