import ballerina/http;
import ballerina/io;

final http:Client foodClient = check new ("http://localhost:9090");

function greetUser(string name) returns string|error {
    string greeting = "Hello, " + name;
    io:println(greeting);
    string response = "Welcome to Ballerina!";
    io:println(response);
    return greeting + " " + response;
}

function getOranges() returns string {
    do {
        json res3 = check foodClient->get("/western/oranges");
        return res3.toString();
    } on fail {
        string msg = "Failed to get the response";
        do {
            json res = check foodClient->post("/log", "Error occurred while getting the response");
            msg = msg.toString() + res.toString();
        } on fail {
            msg = "Error occurred while logging the error";
        }
        return msg;
    }
}

function getPineapples() returns string|http:ClientError? {
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

service /food on new http:Listener(9090) {
    resource function get apples() returns string {
        do {
            json res2 = check foodClient->get("/western/apples");
            return res2.toString();
        } on fail {
            return "Failed to get the response";
        }
    }

    resource function get oranges() returns string|error {
        return getOranges();
    }

    resource function get pineapples() returns string {
        do {
            json res = check foodClient->get("/western/pineapples");
            return res.toString();
        } on fail http:ClientError err {
            return err.message();
        }
    }
}
