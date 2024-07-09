type AssertionError distinct error;

const ASSERTION_ERROR_REASON = "AssertionError";

function panicFunc1() {
    panic error("");
}

function panicFunc2() {
    panic error("AssertionError");
}

function panicFunc3() {
    panic error AssertionError(ASSERTION_ERROR_REASON, message = "Assertion error");
}

function panicFunc4() {
    error e = error("AssertionError");
    panic e;
}
