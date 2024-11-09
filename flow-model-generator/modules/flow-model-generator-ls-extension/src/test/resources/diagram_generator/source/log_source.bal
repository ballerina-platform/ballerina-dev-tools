import ballerina/log;

@display {
    label: "sendEmail"
}
public function main() returns error? {
    do {

    } on fail error e {
        log:printError("Error: ", 'error = e);
        return e;
    }
}
