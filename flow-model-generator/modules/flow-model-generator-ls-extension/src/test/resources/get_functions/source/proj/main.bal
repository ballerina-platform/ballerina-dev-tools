import ballerina/io;

public function main() {
    io:println(greet("World"));
}

function greet(string name) returns string {
    return "Hello, " + name + "!";
}
