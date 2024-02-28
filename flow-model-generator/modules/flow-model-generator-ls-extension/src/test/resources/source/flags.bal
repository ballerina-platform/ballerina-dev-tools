import ballerina/ftp;
import ballerina/http;

public function main() returns error? {
    check fnWithError();
    checkpanic fnWithError();

    boolean res1 = check fnWithErrorAndValue();
    boolean res2 = checkpanic fnWithErrorAndValue();
    final var res3 = check fnWithErrorAndValue();
    final var res4 = checkpanic fnWithErrorAndValue();
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
