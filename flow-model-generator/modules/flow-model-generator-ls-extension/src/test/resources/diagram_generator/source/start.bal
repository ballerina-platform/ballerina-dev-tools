function testStart1() {
    future<int> futureVar = start sum(40, 50);
}

function sum(int i, int j) returns int {
    return i + j;
}

client class Foo1 {
    string name;

    function init(string name) {
        self.name = name;
    }

    remote function getName() returns string {
        return self.name;
    }
}

function testStart2() {
    Foo1 f1 = new("foo");

    future<string> name = start f1->getName();
}
