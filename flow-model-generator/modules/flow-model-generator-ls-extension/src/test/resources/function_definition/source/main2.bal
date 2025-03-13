import ballerina/io;

public function main(string name, int? age) {
    if (age is int) {
        io:println("Hello, " + name + "! You are " + age.toString() + " years old.");
    } else {
        io:println("Hello, " + name + "!");
    }
}
