import ballerina/io;
import ballerina/http;

public function testFunctionCall() {
    int result = 10;
    http:ClientConfiguration config = {
        baseUrl: "http://localhost:9090"
    };
    io:println("Result: ", result);
}

function fn() {

}
