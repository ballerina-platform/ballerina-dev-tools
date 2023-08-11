import ballerina/http;

boolean condition1 = true;
boolean condition2 = true;
boolean condition3 = true;
boolean condition4 = true;
boolean condition5 = true;
boolean condition6 = true;

function testA() returns string {
    string test = testB();
    if (condition1) {
        string test2 = testD();
        if (condition2) {
            string test3 = testE();
            if (condition3) {
                if(condition5) {
                    string test4 = testG();
                }
            }
        }
        if (condition4) {
            string test4 = testF();
        }
    }
    return "";
}

function testB() returns string {
    string return3 = testC();

    return "";
}

function testC() returns string {
    return "";
}

function testD() returns string {
    int a = 1;
    return "";
}

function testE() returns string {
    int a = 1;
    return "";
}

function testF() returns string {
    int a = 1;
    return "";
}

function testG() returns string {
    int a = 1;
    if (condition6) {
        string test = testH();
    }
    return "";
}

function testH() returns string {
    int a = 1;
    return "";
}