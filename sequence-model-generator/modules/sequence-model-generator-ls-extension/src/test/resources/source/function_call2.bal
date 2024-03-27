function fn1() returns int {
    int ret2 = fn2();
    int ret3 = fn3(2);
    return ret2 * ret3;
}

function fn2() returns int {
    int i = fn3(2);
    return 2 + i;
};

function fn3(int i) returns int => 3;
