function testStop1() {
    return;
}

function testStop2(int x) returns int? {
    if x == 2 {
        return;
    } else if x == 3 {
        return x;
    }
    return;
}
