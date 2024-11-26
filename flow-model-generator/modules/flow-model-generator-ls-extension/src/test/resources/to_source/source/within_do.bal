function bar() returns error? {
    string|error foo1 = foo();
    string foo2 = foo();
    string _ = check foo();
}

function baz() returns error? {
    do {
        string|error foo1 = foo();
        string foo2 = check foo();
        string _ = check foo();
    } on fail error er {
        return er;
    }
}

function foo() returns string|error => "foo";
