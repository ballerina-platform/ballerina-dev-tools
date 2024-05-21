function fn1(boolean flag) {
    if flag {
        int a = fn2();
    } else {
        int b = fn3();
        string c = fn4();
    }
}

function fn2() returns int => 2;

function fn3() returns int => 3;

function fn4() returns string => "12";
