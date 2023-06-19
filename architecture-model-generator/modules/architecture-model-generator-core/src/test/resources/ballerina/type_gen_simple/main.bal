import ballerina/io;

public type Employee record {
    readonly string userId;
    string name;
    string jobRole;
    Employer employer;
};

public type Employer record {
    readonly string userId;
    string name;
};

public function main() {
    io:println("Hello, World!");
}
