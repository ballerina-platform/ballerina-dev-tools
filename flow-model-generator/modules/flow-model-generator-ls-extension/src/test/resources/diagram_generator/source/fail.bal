type AssertionError distinct error;

const ASSERTION_ERROR_REASON = "AssertionError";

function failInDoOnFail1() returns error? {
    do {
        fail error("Failing");
    } on fail var e {
        return e;
    }
}

function failInDoOnFail2() returns error? {
    do {
        fail error AssertionError(ASSERTION_ERROR_REASON, message = "Assertion error");
    } on fail var e {
        return e;
    }
}

function failInDoOnFail3() returns error? {
    do {
        AssertionError e = error AssertionError(ASSERTION_ERROR_REASON, message = "Assertion error");
        fail e;
    } on fail var e {
        return e;
    }
}

function failInRetry() returns error? {
    string str = "string";
    int count = 0;
    retry {
        count = count + 1;
        if (count < 5) {
            str += "retry";
        }
        str = "value";
        fail error("Failing");
    } on fail error e {
        return e;
    }
}
