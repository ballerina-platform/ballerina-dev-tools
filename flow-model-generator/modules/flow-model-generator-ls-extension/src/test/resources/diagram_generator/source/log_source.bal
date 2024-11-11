import ballerina/log;

@display {
    label: "sendEmail"
}
public function main() returns error? {
    do {

    } on fail error e {
        log:printError("Error: ", 'error = e, key1="dadfa", key2=123);
        return e;
    }
}
