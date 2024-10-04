function testRetryStatement() returns error? {
    retry {
        check nameOrError();
    }

    retry {
        check nameOrError();
    } on fail var err {
        check err;
    }

    retry (4) {
        check nameOrError();
    } on fail var err {
        check err;
    }

    retry (4) {
        fail error("failed retry");
    } on fail var err {
        check err;
    }

    retry<error:DefaultRetryManager> (4) {
        fail error("failed retry");
    } on fail var err {
        check err;
    }
}

function nameOrError() returns error? => ();
