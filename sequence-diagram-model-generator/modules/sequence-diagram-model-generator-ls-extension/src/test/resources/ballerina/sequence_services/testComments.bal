import ballerina/http;

function testC1() {
    check testC4();
    string a = testC3();
    //@comment-in-sequence: variable initialization
    while(true) {
     int b = 2;
    }
    http:Client diseaseEpB = check new ("https://disease.sh/v3");
    //@comment-in-sequence: action invocation for diseaseEpB->/covid\-19/countries
    Country[] countries = check diseaseEpB->/covid\-19/countries;

    testC2();
}

function testC2() {
    int a = 1;
    if  (a == 1) {
        //@hide-in-sequence: variable initialization
        int b = 2;
    }
}

function testC3() returns string {
    return "testC3";
}

function testC4() returns error? {
    return error("testC4");
}