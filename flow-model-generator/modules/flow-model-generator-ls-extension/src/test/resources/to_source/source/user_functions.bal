
function greet(string name) returns string {
    return "Hello, " + name + "!";
}

function power(int base, int exponent = 2) returns int {
    int result = 1;
    int i = 0;
    while i < exponent {
        result *= base;
        i += 1;
    }
    return result;
}

function asum(int... numbers) returns int {
    int total = 0;
    foreach int num in numbers {
        total += num;
    }
    return total;
}

function operate(int a, int b, function (int, int) returns int func) returns int {
    int[] az = [1, 2];
    int[] bz = [...az];
    return func(a, b);
}
