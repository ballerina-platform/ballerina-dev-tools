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
