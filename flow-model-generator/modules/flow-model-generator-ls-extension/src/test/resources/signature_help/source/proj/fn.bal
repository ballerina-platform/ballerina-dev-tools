import ballerina/io;

# Computes the sum of a variable number of integers and prints the result.
#
# + numbers - The integers to be summed.
public function sumNumbers(int... numbers) {
    int sum = 0;
    foreach int num in numbers {
        sum += num;
    }
    io:println(`Sum: ${sum}`);
}

function processData(int value) returns error? {
    if (value < 0) {
        return error("Invalid input: Value must be non-negative");
    }
    return;
}

# Calculates the square root of a given integer value.
#
# + value - The integer value for which the square root needs to be calculated.
# + return - int|error - The square root of the given value if it is non-negative, or an error if the value is negative.
#
# ### Errors
# - Returns an error if the input value is negative.
function calculateSquareRoot(int value) returns int|error {
    if (value < 0) {
        return error("Invalid input: Cannot calculate square root of a negative number");
    }
    return value / 2;
}
