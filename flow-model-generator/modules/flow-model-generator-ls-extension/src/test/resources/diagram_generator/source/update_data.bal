function testNewData() {
    int i = 2;
    i = 3;
    _ = 4;
}

function testNewDataString() {
    string str1 = "Hello world";
    str1 = string `${str1} ${str1}`;
}

function testNewDataJson() {
    json j1 = {"name": "John", "age": 34};
    j1 = {"name": "Marry", "age": 33};
}

function testNewDataXml() {
    xml x1 = xml `<p>content</p>`;
    x1 = xml `<book>The Lost World</book>`;
}

function testNewDataJson3(int amount) returns error? {
    json newData1 = [check calculate(amount), check calculate(amount + 1)];
    newData1 = [check calculate(amount), check calculate(amount + 1)];
    [int, int] newData2 = [1, 2];
    newData2 = [11, 22];
}

function calculate(int i) returns int|error {
    return 3;
}
