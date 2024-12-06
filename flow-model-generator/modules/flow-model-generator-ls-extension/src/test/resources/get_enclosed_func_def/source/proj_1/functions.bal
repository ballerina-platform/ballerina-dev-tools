function foo() returns string {
    int i;
    for (int i = 0; i < 10; i++) {
        i = i + 1;
    }
    return "Hello " + i.toString();
}

function bar() returns string {
    return foo();
}

function baz() returns string {
    int a = 1;
    int b = 2;
    return a + b;
}
