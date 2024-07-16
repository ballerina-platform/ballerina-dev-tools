function testLock1() {
    lock {
        int i = 0;
    }
}

function testLock2() {
    lock {
        int i = 0;
        lock {
            i = 3;
        }
    }
}

function testLockWithOnFail1() returns error? {
    lock {
        int i = 0;
    } on fail {
        return;
    }
}

function testLockWithOnFail2() returns error? {
    lock {
        fail error("Error Message");
    } on fail var e {
        return e;
    }
}
