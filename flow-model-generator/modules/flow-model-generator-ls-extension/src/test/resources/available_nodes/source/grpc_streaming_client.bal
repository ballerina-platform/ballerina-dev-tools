import ballerina/grpc;
import ballerina/log;

final grpc:StreamingClient moduleClient = new;

public function main() returns error? {
    grpc:Error? send = moduleClient->send("Hello");
    if send is grpc:Error {
        log:printError("error occurred", 'error = send);
    }

}
