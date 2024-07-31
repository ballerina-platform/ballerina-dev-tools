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

function testNewDataJson() {
    json j1 = {"name": "John", "age": 34};
}

function testNewDataXml() {
    string content = "create xml";
    xml x1 = xml `<p>${content}</p>`;
    xml x2 = xml `<book>The Lost World</book>`;
}

function testJson(int amount) returns error? {
    json newData = {value: check calculate(amount)};
}

function calculate(int i) returns int|error {
    return 3;
}
