import project.mod1;

public function main() {
    _ = mod1:isEven(2);
    _ = isOdd(3);
}

function isOdd(int num) returns boolean => num % 2 == 1;
