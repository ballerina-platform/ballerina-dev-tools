type Food record {|
    int price;
    int remaining;
|};

function fn1(Food food) {
    int a;
    int b;
    string c;
    if food.price < 100 {
        a = fn2();
    } else if food.price < 200 {
        if food.remaining > 10 {
            a = fn2();
            c = fn4();
        } else {
            b = fn2();
        }
        b = fn3();
    } else {
        c = fn4();
    }
    a = fn2();
}

function fn2() returns int => 2;

function fn3() returns int => 3;

function fn4() returns string => "12";
