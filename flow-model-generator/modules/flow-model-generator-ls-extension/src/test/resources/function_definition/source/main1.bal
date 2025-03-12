import ballerina/io;

public function main(int divisor, float... numbers) returns error? {
    float sum = numbers.reduce(function(float f, float f1) returns float {
        return f + f1;
    }, 0);
    if (divisor == 0) {
        return error("Divisor cannot be zero");
    }
    float result = sum / divisor;
    io:println("The result is ", result);
}
