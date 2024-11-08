import ballerina/data.jsondata;
import ballerina/http;
import ballerina/io;
import ballerina/log;

final http:Client foodClient = check new ("http://localhost:9090");

service on new http:Listener(8080) {
    resource function get apples(int count) {
        if count > 20 {
            log:printWarn("Count is greater than 20");
        }
        log:printInfo("Getting apples", key1="Value1", key2="Value2");
        json|error res = foodClient->get("/western/apples?count=" + count.toString());
        if res is error {
            log:printError("Failed to get the response");
        } else {
            log:printInfo("Response: ", response = res);
        }
    }

    resource function post apples(string payload) returns Apple|jsondata:Error {
        json jsonResult = check jsondata:toJson(payload);
        Apple apple = check jsondata:parseAsType(jsonResult);
        return apple;
    }
}

type Apple record {|
    string title;
    string author;
|};

function greet(string name) returns string {
    return "Hello, " + name + "!";
}

function power(int base, int exponent = 2) returns int {
    int result = 1;
    int i = 0;
    while i < exponent {
        result *= base;
        i += 1;
    }
    return result;
}

function asum(int... numbers) returns int {
    int total = 0;
    foreach int num in numbers {
        total += num;
    }
    return total;
}

function operate(int a, int b, function (int, int) returns int func) returns int {
    int[] az = [1, 2];
    int[] bz = [...az];
    return func(a, b);
}

function print() {

}

function printArg(string val) => ();

public function main() {
    string greeting = greet("Alice");
    int squared = power(3);
    int cubed = power(3, 3);
    io:println("3 squared: ", squared, ", 3 cubed: ", cubed);
    int total = asum(1, 2, 3, 4, 5);

    int result = operate(6, 3, function(int x, int y) returns int {
                return x * y;
            });
    io:println("6 * 3 = ", result);
    io:println(greeting + total.toString());
    print();
}
