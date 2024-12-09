import ballerina/log;

@display {
    label: "Order Dispatcher"
}
public function main() returns error? {
    do {
        check foo();
    } on fail error e {
        log:printError("Error: ", 'error = e);
        return e;
    }
}