// Basic function with no parameters and no return value
public function simpleFunction() {
    // Function body
}

// Function with parameters and return type
public function add(int a, int b) returns int {
    return a + b;
}

// Function with optional parameters
public function greet(string name, string title = "Mr.") returns string {
    return string `Hello ${title} ${name}!`;
}

// Function with rest parameters
public function sum(int... numbers) returns int {
    int total = 0;
    foreach int num in numbers {
        total += num;
    }
    return total;
}

// Function with all 
public function complexFunction(int required, string name = "default", boolean flag = false, int... rest) returns string {
    string result = string `Required: ${required}, Name: ${name}, Flag: ${flag}, Rest: `;
    foreach int num in rest {
        result += num.toString() + " ";
    }
    return result;
}

// Function with default return value
public function divide(int a, int b) returns int|error {
    if b == 0 {
        return error("Division by zero");
    }
    return a / b;
}

// Isolated function
isolated function getCounter() returns int {
    return 42;
}

// Function with record type parameter
public function processUser(record {|string name; int age;|} user) returns string {
    return string `${user.name} is ${user.age} years old`;
}

// Function name with special characters
public function 'from() {
}

public function \$dollar\#hash() {
}

public function ව() {
}
