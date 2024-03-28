function fn1(int price) {
    if price > 10 {
        int a = 1;
    } else if price > 20 {
        int b = fn2();
    } else {
        int c = 3;
    }
}

function fn2() returns int => 2;

