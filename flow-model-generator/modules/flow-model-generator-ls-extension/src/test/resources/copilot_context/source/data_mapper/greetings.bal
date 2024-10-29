import ballerina/http;
import ballerina/io;

service / on new http:Listener(8080) {
    resource function get greeting() returns string {

    }
}
