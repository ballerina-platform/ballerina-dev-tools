import ballerina/log;

@display {
    label: "DeleteAutomata3",
    cron: ""
}
public function main() returns error? {
    do {
        log:printDebug("Automation works", k = 30);
    } on fail error e {
        log:printError("Error: ", 'error = e);
        return e;
    }
}

type Person record {
    string name;
    int age;
    string city;
    boolean isStudent;
};
