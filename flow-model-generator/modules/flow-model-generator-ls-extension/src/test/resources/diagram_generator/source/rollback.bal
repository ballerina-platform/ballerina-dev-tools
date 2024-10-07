function testRollback(int? i) returns error? {
    transaction {
        if i is () {
            check commit;
        } else {
            rollback;
        }
    }
}

function testRollbackWithExpression(int? i) returns error? {
    transaction {
        if i is () {
            check commit;
        } else {
            rollback error("Rollback the transaction");
        }
    }
}
