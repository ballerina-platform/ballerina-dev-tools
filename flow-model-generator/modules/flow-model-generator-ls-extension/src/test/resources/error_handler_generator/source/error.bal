import ballerina/http;
import ballerina/io;

final http:Client foodClient = check new ("http://localhost:9090");

function greetUser(string name) returns string {
    string greeting = "Hello, " + name;
    io:println(greeting);
    string response = "Welcome to Ballerina!";
    io:println(response);
    return greeting + " " + response;
}

function exitUser(string name) returns string[]|boolean {
    string farewell = "Goodbye, " + name;
    io:println(farewell);
    string[] responses = ["Goodbye", "See you later"];
    return responses;
}

function getPineapples() returns string? {
    string? msg;
    do {
        json res = check foodClient->get("/western/pineapples");
        msg = res.toString();
        return msg;
    } on fail http:ClientError err {
        msg = err.message();
        return msg;
    }
}

function emptyFn() {

}

service /food on new http:Listener(9090) {

    resource function get pineapples() returns string? {
        return getPineapples();
    }
}
