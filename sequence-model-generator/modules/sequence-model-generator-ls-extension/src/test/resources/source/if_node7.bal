function fn1(int price) {
    if price > 10 {
        int a = 1;
    } else {
        int c = fn2();
    }
}

function fn2() returns int => 2;

