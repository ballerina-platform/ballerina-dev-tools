import ballerina/io;

class EvenNumber {
    int i = 1;

    isolated function generate() returns int {
        lock {
            return self.i * 2;
        }
    }
}

public function main() {
    //@sq-comment: EvenNumber initialization
    EvenNumber e = new;
    //@sq-comment: while loop
    while  (5  == e.generate()) {
        testFun();
    }

    //@sq-ignore
    int numVal = getNum1();

    testWithParam(getNum1(),getNum2());
}

public function generate() {
    EvenNumber e = new;
    int c = e.generate();
    io:println(c);
}

public function testWithParam(int a, int b) {
    int c = a + b;
}

public function getNum1() returns int {
    return 1;
}

public function getNum2() returns int {
    return 1;
}

public function getNum3() returns int {
    return 1;
}

public function testFun() {
    if  (1 == getNum2()) {
        int f = getNum3();
    } else {
        int y = getNum1();
    }
}
