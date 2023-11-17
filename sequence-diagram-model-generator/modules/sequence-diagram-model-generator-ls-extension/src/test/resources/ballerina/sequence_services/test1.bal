import sequence_services.testModule;

boolean condition1 = true;
boolean condition2 = false;

function testA1() returns string {
    string test = testB1();
    if (condition1) {
        string test2 = testC1();
    } else {
        string test3 = testF1();
        string test4 = testE1();
        testModule:hello();
    }
    return "";
}   

function testB1() returns string {
    while (condition2) {
        string test5 = testA1();
    }
    string[] names = ["Bob", "Jo", "Ann", "Tom"];
    foreach string name in names {
        string test6 = testH1();
    }
    string rr = testC1();
    string returnVal = "";
    return returnVal;
}


type RC1 record {
    string name;
    string address;
};

type RC2 record {
    string name;
    int age;
};

function testC1() returns string {
    string returnC = "";
    return returnC;
}

function testE1() returns RC1|RC2 {
    RC1 rc1 = {name: "Bob", address: "123 Main St."};
    return rc1;
}

function testF1() returns string {
    return "";
}

function testH1() returns string {
    string dd = testH1();
    return "";
}
