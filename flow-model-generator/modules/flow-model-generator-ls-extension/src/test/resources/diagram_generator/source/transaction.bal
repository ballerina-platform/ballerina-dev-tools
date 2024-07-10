function testTransaction1() returns error? {
    transaction {
        var res = check commit;
    }
}

function testTransaction2(int i) returns int|error {
    int x = i;

    transaction {
        var res = check commit;
    }

    transaction {
        var res = check commit;
    }
    return x;
}

function testTransaction3() returns error? {
    transaction {
        transaction {
            var res = check commit;
        }
        var res = check commit;
    }
}

function transactionWithOnFail1() returns error? {
    transaction {
        var _ = check commit;
    } on fail {
        return error("Found error");
    }
}

function transactionWithOnFail2() returns error? {
    transaction {
        var _ = check commit;
    } on fail error e {
        return e;
    }
}

function transactionWithOnFail3() returns error? {
    transaction {
        transaction {
            var _ = check commit;
        }
        var _ = check commit;
    } on fail error e {
        return e;
    }
}
