function fn1(int price) {
    if price > 10 {
        int a = fn2();
    } else if price > 20 {
        int b = 12;
    } else {
        int c = fn2();
    }
}

function fn2() returns int => 2;

