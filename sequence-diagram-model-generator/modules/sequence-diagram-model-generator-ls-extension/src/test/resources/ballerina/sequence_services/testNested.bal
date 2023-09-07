import ballerina/http;

function testD6() {
//@sq-comment: logic for A
//@sq-comment: logic for B
//@sq-ignore
check testD4();
do{
check testD5();
} on fail var e {
http:Client diseaseEpB = check new ("https://disease.sh/v3");
            //@sq-ignore
            Country[] countries = check diseaseEpB->/covid\-19/countries;
}
    




}

function testD2() {
    int a = 1;
    testD5();
}

function testD3() returns string {
    return "testD3";
}

function testD4() returns error? {
    return error("testD4");
}

function testD5() {
    int c = 2;
}
