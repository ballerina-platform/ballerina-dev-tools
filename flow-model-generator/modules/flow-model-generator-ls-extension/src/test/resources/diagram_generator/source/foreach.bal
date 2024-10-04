public function testForeach1() {
    int[] intArr = [1, 2, 3];
    foreach var item in intArr {
        _ = item;
    }
}

public function testForeach2() {
    int[] intArr = [1, 2, 3];
    foreach int item in intArr {
        _ = item;
    }
}

public function testForeachWithOnFail1() returns error? {
    int[] intArr = [1, 2, 3];
    foreach int item in intArr {
        _ = item;
    } on fail {
        return error("Error message");
    }
}

public function testForeachWithOnFail2() returns error? {
    int[] intArr = [1, 2, 3];
    foreach int item in intArr {
        _ = item;
    } on fail var e {
        return e;
    }
}

public function testForeach3() returns error? {
    int[]|error intArr = [1, 2, 3];
    foreach int item in check intArr {
        _ = item;
    }
}
