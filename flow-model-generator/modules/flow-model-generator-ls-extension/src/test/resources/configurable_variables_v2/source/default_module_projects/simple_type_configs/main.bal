import ballerina/io;

configurable int fixedDiscount = -200;
configurable decimal discountPercentage = ?;
configurable boolean testMode = true;

public function main() {
    io:println("Hello, World!");
}
