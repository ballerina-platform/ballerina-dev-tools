import ballerina/time;

public function main() {
}

isolated function getDefaultDob() returns time:Utc {
    do {
	    return check time:utcFromString("2019-01-01T00:00:00Z");
    } on fail {
        return [1, 1];
    }
}
