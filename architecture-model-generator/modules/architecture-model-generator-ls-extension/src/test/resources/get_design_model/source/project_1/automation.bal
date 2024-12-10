import ballerina/http;
import ballerina/log;

@display {
    label: "Order Dispatcher"
}
public function main() returns error? {
    do {
        final http:Client httpClient = check new ("");
        json _ = check httpClient->/;
        check foo();
    } on fail error e {
        log:printError("Error: ", 'error = e);
        return e;
    }
}