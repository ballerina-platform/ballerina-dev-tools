boolean condition1 = true;
boolean condition2 = true;
boolean condition3 = true;
boolean condition4 = true;
boolean condition5 = true;

function testA1() returns string {
    string test = testB1();
    if (condition1) {
        string test2 = testC1();
    } else if (condition2) {
        string test4 = testD1();
    } else if (condition3){
        string test5 = testE1();
    } else {
        string test6 = testF1();
    }
    return "";
}

function testB1() returns string {
    while (condition4) {
        string test3 = testA1();
    }
    string[] names = ["Bob", "Jo", "Ann", "Tom"];
    // Loop through a list.
    foreach string name in names {
        string test = testH1();
    }
    return "";
}

function testC1() returns string {
    return "";
}

function testD1() returns string {
    return "";
}

function testE1() returns string {
    return "";
}

function testF1() returns string {
    return "";
}

function testH1() returns string {
    return "";
}