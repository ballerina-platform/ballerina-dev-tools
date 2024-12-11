import ballerina/http;
import ballerina/log;

@display {
    label: "BIT Service"
}
service /api/v1 on new http:Listener(9092) {

    private http:Client httpClient = check new ("");
    private http:Client httpClient2;
    private http:Client httpClient3 = httpClient;

    function init() returns error? {
        self.httpClient2 = check new("");
    }

    resource function get greeting() returns json|http:InternalServerError {
        do {
            json j = check httpClient->/;
            http:Client httpClient3 = self.httpClient2;
            check self.foo3(httpClient3);
            log:printInfo(j.toJsonString());
        } on fail error e {
            log:printError("Error: ", 'error = e);
            return http:INTERNAL_SERVER_ERROR;
        }
    }

    function foo1() returns error? {
        json j = check self.httpClient->/["path"].post({});
        json j2 = check self.httpClient2->/["path"].post({});
        json j3 = check self.httpClient3->/["path"].post({});
    }

    function foo2() returns error? {
        http:Client httpClient10 = check new("");
        json j = check httpClient10->/;
    }

    function foo3(http:Client httpClient) returns error? {
        json j = check httpClient->/;
    }
}
