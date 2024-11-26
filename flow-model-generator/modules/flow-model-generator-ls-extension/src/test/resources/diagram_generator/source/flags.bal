import ballerina/ftp;
import ballerina/http;

public function main() returns error? {
    check fnWithError();
    checkpanic fnWithError();

    boolean _ = check fnWithErrorAndValue();
    boolean _ = checkpanic fnWithErrorAndValue();
    final var _ = check fnWithErrorAndValue();
    final var _ = checkpanic fnWithErrorAndValue();
}

service /rem on new ftp:Listener({}) {
    remote function onFileChange(ftp:Caller caller, ftp:WatchEvent & readonly event) returns ftp:Error? {

    }
}

service /res on new http:Listener(0) {
    resource function accessor path() {

    }
}

// Utility functions
function fnWithError() returns error? => error("error message");

function fnWithErrorAndValue() returns boolean|error => false;

function fnWithPanic() {
    panic error("error message");
}
