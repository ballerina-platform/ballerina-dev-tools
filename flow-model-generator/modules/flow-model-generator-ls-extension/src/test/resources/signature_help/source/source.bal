import ballerina/io;

# Provides information about a person.
#
# + name - The name of the person
# + age - The age of the person (default is 25)
public function info(string name, int age = 25) {
    io:println(`Name: ${name}, Age: ${age}`);
}

# Multiplies the given integer value by two.
#
# + value - The integer value to be multiplied.
# + return - The result of the multiplication.
function multiplyByTwo(int value) returns int {
    return value * 2;
}

public function main() {
    info("John");
    
}

# Generates a token based on the provided ID and timestamp.
# 
# + id - The ID to be used in the token generation.
# + timestamp - The timestamp to be used in the token generation.
# + return - The generated token
function getToken(string id, string timestamp) returns string => string `${id}-$timestamp`;
