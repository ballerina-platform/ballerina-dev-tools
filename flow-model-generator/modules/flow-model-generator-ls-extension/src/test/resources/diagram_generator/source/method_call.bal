class Calculator {
    private int total = 0;

    function init(int initialValue = 0) {
        self.total = initialValue;
    }

    public function add(int value) returns int {
        self.total += value;
        return self.total;
    }

    public function multiply(int value) returns int {
        self.total *= value;
        return self.total;
    }

    public function getTotal() returns string {
        return string `Total: ${self.total}`;
    }

    public function clear() {
        self.total = 0;
    }
}

public function userMethods() {
    // Create an instance of Calculator class
    Calculator calc = new Calculator(10);

    // Method call examples
    int result1 = calc.add(5);
    int result2 = calc.multiply(2);
    string total = calc.getTotal();
    calc.clear();
}

public function langLibMethods() {
    int[] numbers = [1, 2, 3, 4, 5];
    int sum = numbers.reduce(function(int total, int n) returns int {
        return total + n;
    }, 0);

    string text = "Hello Ballerina";
    string upper = text.toUpperAscii();
    boolean contains = text.includes("Ball");

    map<string> fruits = {
        "a": "apple",
        "b": "banana"
    };
    string[] keys = fruits.keys();
    int|error val = int:fromString("100");
}

