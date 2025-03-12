import ballerina/data.jsondata;
import ballerina/io;

public function main() {
    io:println(greet("World"));
    record {|string name;|}|error val = jsondata:parseString("{\"name\":\"Foo\"}");
}

function greet(string name) returns string {
    return "Hello, " + name + "!";
}




function sum2(int... numbers) returns int {
    int total = 0;
    foreach var num in numbers {
        total += num;
    }
    return total;
}