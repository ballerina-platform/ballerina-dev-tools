function fn1(boolean flag) {
    if flag {
        int a = fn2();
    } else {
        int b = 12;
    }
}

function fn2() returns int => 2;

