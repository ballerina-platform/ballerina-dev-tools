function testRetryTransactionStatement() returns error? {
    retry transaction {
        check nameOrError();
        check commit;
    }

    retry transaction {
        check nameOrError();
        check commit;
    } on fail var err {
        check err;
    }

    retry (4) transaction {
        check nameOrError();
        check commit;
    } on fail var err {
        check err;
    }

    retry (4) transaction {
        if true {
            check commit;
        }
        fail error("failed retry");
    } on fail var err {
        check err;
    }

    retry<error:DefaultRetryManager> (4) transaction {
        if true {
            check commit;
        }
        fail error("failed retry");
    } on fail var err {
        check err;
    }
}

function nameOrError() returns error? => ();
