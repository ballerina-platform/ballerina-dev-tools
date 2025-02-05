import ballerina/io;

public function main(string name) {
    io:println(string `Hello ${name}!`);
}
