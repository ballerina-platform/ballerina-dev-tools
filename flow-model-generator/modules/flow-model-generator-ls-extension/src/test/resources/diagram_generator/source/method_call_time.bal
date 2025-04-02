import ballerina/time;

public function main() returns error? {
    time:Zone timeZone = check time:loadSystemZone();
    time:ZoneOffset|() offeset = timeZone.fixedOffset();
}
