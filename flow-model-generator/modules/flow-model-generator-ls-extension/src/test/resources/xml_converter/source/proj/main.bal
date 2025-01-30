import ballerina/io;

public function main() {
    io:println();
}

function greet(string name) returns string {
    return "Hello, " + name + "!";
}
