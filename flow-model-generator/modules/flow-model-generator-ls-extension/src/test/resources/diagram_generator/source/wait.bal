import ballerina/io;
import ballerina/lang.runtime;

// Mock function to simulate network calls with delays
function mockFetch(string url, decimal delay = 1) returns string|error {
    runtime:sleep(delay);
    if (url.includes("error")) {
        return error("Failed to fetch: " + url);
    }
    return "Response from: " + url;
}

// Simple wait with string return
function fetchExample(decimal delay = 1) returns string {
    future<string|error> futureResult = start mockFetch("http://example.com", delay);
    string|error result = wait futureResult;
    if result is error {
        return "Failed to fetch";
    }
    return result;
}

// Simple wait example
function simpleWait() {
    future<MultiResult> futureResult = start multipleWaitWithRecord();
    MultiResult|error result = wait futureResult;
}

// Simple wait for an imported function
function waitForImportedFunction() returns error? {
    future<string|error> futureResult = start io:readln("Enter a value: ");
    string result = check wait futureResult;
    io:println("You entered: " + result);
}

// Basic alternate wait with two workers
function basic2AlternateWait() returns string|error {
    worker A returns string|error {
        return mockFetch("http://a.com", 2);
    }

    worker B returns string|error {
        return mockFetch("http://b.com", 1);
    }

    string|error result = wait A | B;
    return result;
}

// Basic alternate wait with three workers
function basic3AlternateWait() returns string|error {
    worker A returns string|error {
        return mockFetch("http://a.com", 2);
    }

    worker B returns string|error {
        return mockFetch("http://b.com", 1);
    }

    worker C returns string|error {
        return mockFetch("http://c.com", 3);
    }

    string|error result = wait A | B | C;
    return result;
}

// Multiple wait with record
type MultiResult record {
    string|error a;
    string|error b;
    string|error c;
};

function multipleWait() returns map<string|error> {
    worker WA returns string|error {
        return mockFetch("http://a.com", 1);
    }

    worker WB returns string|error {
        return mockFetch("http://b.com", 2);
    }

    worker WC returns string|error {
        return mockFetch("http://error.com", 3);
    }

    map<string|error> mapResult = wait {WA, WB, WC};
    return mapResult;
}

function multipleWaitWithRef() returns map<string|error> {
    worker WA returns string|error {
        return mockFetch("http://a.com", 1);
    }

    worker WB returns string|error {
        return mockFetch("http://b.com", 2);
    }

    worker WC returns string|error {
        return mockFetch("http://error.com", 3);
    }

    map<string|error> mapResult = wait {wa: WA, WB, wC: WC};
    return mapResult;
}

function multipleWaitWithRecord() returns MultiResult {
    worker WA returns string|error {
        return mockFetch("http://a.com", 1);
    }

    worker WB returns string|error {
        return mockFetch("http://b.com", 2);
    }

    worker WC returns string|error {
        return mockFetch("http://error.com", 3);
    }

    MultiResult mapResult = wait {a: WA, b: WB, c: WC};
    return mapResult;
}

// Wait with worker cancellation
function waitWithCancellation() returns string[] {
    string[] results = [];

    worker WA {
        runtime:sleep(1);
        results.push("WA");
    }

    worker WB {
        runtime:sleep(.5);
        results.push("WB");
    }

    // Only wait for WB
    wait WB;
    return results;
}

// Sequential wait with dependencies
function sequentialWaitWithDependencies() returns string[] {
    string[] results = [];

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

    wait Third;
    return results;
}

// Wait with mixed return types
type MixedResult record {
    int a;
    string b;
    boolean c;
};

function waitWithMixedTypes() returns MixedResult {
    worker WA returns int {
        runtime:sleep(1);
        return 42;
    }

    worker WB returns string {
        runtime:sleep(2);
        return "Hello";
    }

    worker WC returns boolean {
        runtime:sleep(1);
        return true;
    }

    MixedResult mapResult = wait {a: WA, b: WB, c: WC};
    return mapResult;
}

// Dynamic wait patterns
function dynamicWaitPatterns(boolean condition) returns error? {
    worker W1 returns string|error {
        return mockFetch("http://1.com", 1);
    }

    worker W2 returns string|error {
        return mockFetch("http://2.com", 2);
    }

    string value;
    if condition {
        value = check wait W1;
    } else {
        value = check wait W2;
    }
    io:println("Value: " + value);
}

// Accessing an future array
function accessFutureArray() returns any {
    future<string> futureResult = start fetchExample();
    future<string> futureResult1 = start fetchExample(1);
    future<string|error> futureResult2 = start mockFetch("http://error.com", 1);
    future<string|error>[] futures = [futureResult, futureResult1, futureResult2];

    string|error val1 = wait futures[0] | futures[2];
    map<string|error> mapResult = wait {val3: futures[0], val4: futures[2]};

    return [val1, mapResult];
}
