import ballerina/io;
import ballerina/lang.runtime;

// Wait with alternate wait
function basicAlternateWait() returns string|error {
    fork {
        worker A returns string|error {
            return "A";
        }

        worker B returns string|error {
            return "B";
        }

        worker C returns string|error {
            return "C";
        }
    }

    string|error result = wait A | B | C;
    return result;
}

function basicAlternateWaitWithOneOut() returns string|error {
    fork {
        worker A returns string|error {
            return "A";
        }

        worker B returns string|error {
            return "B";
        }
    }

    worker C returns string|error {
        return "C";
    }

    string|error result = wait A | B | C;
    return result;
}

// Wait with worker cancellation
function waitWithCancellation() returns string[] {
    string[] results = [];

    fork {
        worker WA {
            runtime:sleep(1);
            results.push("WA");
        }

        worker WB {
            runtime:sleep(.5);
            results.push("WB");
        }
    }

    // Only wait for WB
    wait WB;
    return results;
}

// Sequential wait with dependencies
function sequentialWaitWithDependencies() returns string[] {
    string[] results = [];

    fork {
        worker First {
            runtime:sleep(.5);
            results.push("First done");
        }

        worker Second {
            wait First;
            results.push("Second after First");
        }

        worker Third {
            wait Second;
            results.push("Third after Second");
        }
    }

    wait Third;
    return results;
}

// Workers with init statements
function workersWithInitStatements() returns string? {
    int i = 12;
    string s = "12";

    fork {
        worker A returns string {
            return "hello" + s;
        }
    }

    worker B {
        io:println("Worker B");
    }

    worker C {
        io:println("Worker C");
    }

    string? value = wait A | B | C;
    return value;
}

function workersWithFork() returns string? {
    int i = 12;
    string s = "12";

    worker B {
        io:println("Worker B");
    }

    worker C {
        io:println("Worker C");
    }

    fork {
        worker A returns string {
            return "hello" + s;
        }
    }

    string? value = wait A | B | C;
    return value;
}
