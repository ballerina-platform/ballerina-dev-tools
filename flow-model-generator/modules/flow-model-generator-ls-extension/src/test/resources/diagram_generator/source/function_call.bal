import ballerina/http;
import ballerina/log;

final http:Client foodClient = check new ("http://localhost:9090");

service on new http:Listener(8080) {
    resource function get apples(int count) {
        if count > 20 {
            log:printWarn("Count is greater than 20");
        }
        log:printInfo("Getting apples");
        json|error res = foodClient->get("/western/apples?count=" + count.toString());
        if res is error {
            log:printError("Failed to get the response");
        } else {
            log:printInfo("Response: ", response = res);
        }
    }
}
