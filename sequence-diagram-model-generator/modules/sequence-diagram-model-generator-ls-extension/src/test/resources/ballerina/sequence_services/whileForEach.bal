import ballerina/http;

function testA() returns string{
    while(true) {
    string s = testB()
    string[] names = ["Bob", "Jo", "Ann", "Tom"];
        foreach string name in names {
          string g =  testE();
        }
        }
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
    while(true) {
        string test = testK();
    }
    return "";
}

function testM() returns string{
    return "";
}

