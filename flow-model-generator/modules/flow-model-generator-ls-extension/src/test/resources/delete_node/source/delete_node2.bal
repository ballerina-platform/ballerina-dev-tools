import ballerina/log;

public function deleteNodeWithImport1(int count) {
    if count > 20 {
        log:printWarn("Count is greater than 20");
    }
}
