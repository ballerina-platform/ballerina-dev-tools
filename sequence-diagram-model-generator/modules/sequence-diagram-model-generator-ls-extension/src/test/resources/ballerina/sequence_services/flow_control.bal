import ballerina/http;

boolean condition1 = true;
boolean condition2 = true;
boolean condition3 = true;
boolean condition4 = true;
boolean condition5 = true;
boolean condition6 = true;

function testA() returns string {
    string test = testB();
    http:Client diseaseEpB = check new ("https://disease.sh/v3");
    Country[] countries = check diseaseEpB->/covid\-19/countries;
    if (condition1) {
        string test = testG();
    }
    return "";
}

function testB() returns string {
    if (condition1) {
        string test = testG();
    }
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