import ballerina/io;

// Basic function with no parameters and no return value
public function sayHello() {
    io:println("Hello, World!");
}

// Function with parameters
public function greet(string name) {
    io:println("Hello, " + name + "!");
}

// Function with return value
public function add(int a, int b) returns int {
    return a + b;
}

// Function with multiple return values
public function divideAndRemainder(int dividend, int divisor) returns [int, int] {
    int quotient = dividend / divisor;
    int remainder = dividend % divisor;
    return [quotient, remainder];
}

// Function with default parameter values
public function calculateInterest(decimal principal, decimal rate, int years = 1) returns decimal {
    return principal * rate * years / 100;
}

// Function with rest parameters
public function sum(int... numbers) returns int {
    int total = 0;
    foreach int num in numbers {
        total += num;
    }
    return total;
}

// Function with error handling
public function divide(int a, int b) returns float|error {
    if (b == 0) {
        return error("Division by zero");
    }
    return <float>a / <float>b;
}


function fn\#with\-Identifiers() {

}

// Main function
public function main() {
    sayHello();
    greet("Ballerina");

    int result = add(5, 3);
    io:println("5 + 3 = " + result.toString());

    decimal interest = calculateInterest(1000, 5.5);
    io:println("Interest: " + interest.toString());

    int total = sum(1, 2, 3, 4, 5);
    io:println("Sum: " + total.toString());

    var divResult = divide(10, 2);
    if (divResult is float) {
        io:println("10 ÷ 2 = " + divResult.toString());
    } else {
        io:println("Error: " + divResult.message());
    }
}
