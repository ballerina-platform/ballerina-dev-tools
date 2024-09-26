function add(int a, int b) returns int => a + b;

function sum(int... numbers) returns int {
    int total = 0;
    foreach var num in numbers {
        total += num;
    }
    return total;
}

public function safeDivide(float a, float b) returns float|error {
    if (b == 0.0) {
        return error("Division by zero");
    }
    return a / b;
}
