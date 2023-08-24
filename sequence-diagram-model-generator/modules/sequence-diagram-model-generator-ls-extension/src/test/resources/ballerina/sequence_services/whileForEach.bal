import ballerina/http;

function testA() returns string{
       string test = testC();
       //string test2 = testB();
        string test3 = testK();
    return "";
}

function testB() returns string{
    while(true) {
    string res1 = testC();
    if(false){
    string res2 = testD();
    } else {
        string res3 = testF();
    }
    }
    string[] names = ["Bob", "Jo", "Ann", "Tom"];
    // Loop through a list.
    foreach string name in names {
        string test = testD();
    }

    return "";
}

function testC() returns string{
    return "";
}

function testD() returns string{
    return "";
}

function testE() returns string{
    if(false) {
        string test = testD();
    }  else if (true) {
        string test4 = testF();
        if(false) {
            string test5 = testI();
        } else {
            string test6 = testJ();
        }
    } else {
        string test7 = testC();
    }
    return "";
}

function testF() returns string{

    return "";
}

function testI() returns string{
    return "";
}

function testJ() returns string{
    return "";
}

function testK() returns string{
    if  (false) {
        string test = testL();
    }
    return "";
}

function testL() returns string{
    return "";
}

function testM() returns string{
    return "";
}

