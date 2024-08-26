import ballerina/log;

public function deleteNodeWithImport2(int count) {
    if count > 20 {
        log:printWarn("Count is greater than 20");
    }
    if count <= 20 {
        log:printWarn("Count is less or equal than 20");
    }
}
