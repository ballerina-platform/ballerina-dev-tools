function fn1() {
    int count = 0;
    while count < 100 {
        int val = fn2(count);
        count += val;
    }
}

function fn2(int i) returns int => i * 2;
