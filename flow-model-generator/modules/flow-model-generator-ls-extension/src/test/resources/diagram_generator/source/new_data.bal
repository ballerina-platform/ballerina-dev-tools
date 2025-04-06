function testNewData() {
    int i = 2;
    float f = 1.2f;
    decimal d = 100000.0d;
    decimal d1 = d;

    int j = 3;
    int k = i + j;
    var l = i + j;
    var _ = k + l;
}

function testNewDataString() {
    string str1 = "Hello world";
    string str2 = string `${str1} ${str1}`;
}

function testNewDataJson1() {
    json j1 = {"name": "John", "age": 34};
}

function testNewDataXml() {
    string content = "create xml";
    xml x1 = xml `<p>${content}</p>`;
    xml x2 = xml `<book>The Lost World</book>`;
}

function testNewDataJson2(int amount) returns error? {
    json newData = {value: check calculate(amount)};
}

function testNewDataJson3(int amount) returns error? {
    json newData1 = [check calculate(amount), check calculate(amount + 1)];
    [int, int] newData2 = [1, 2];
    [int, int] newData3 = [check calculate(amount), check calculate(amount + 1)];
}

function testNewDataWithoutExpression() returns error? {
    int a;
    string s;
    json j;
    xml x;

    a = 5;
    s = "Ballerina";
    j = {"key": "value"};
}

function calculate(int i) returns int|error {
    return 3;
}

function invalidType() {
    str s = "Hello world";
}
