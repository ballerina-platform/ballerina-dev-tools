import ballerina/http;
import ballerina/log;

public function deleteNodeWithImport1(int count) returns error? {
    if count > 20 {
        log:printWarn("Count is greater than 20");
    } else {
        http:Client foodClient = check new ("/food");
    }
}
