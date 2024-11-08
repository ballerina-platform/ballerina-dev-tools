import ballerina/io;

public function deleteFunction(int x, int y) returns int {
    return x + y;
}

public function deleteFunctionWithImport1(string name, int age) {
    int ageIn10Years = deleteFunction(age, 10);
    io:println(name + " " + ageIn10Years.toString());
}

public function deleteNodeWithImport2(int count) {
    if count > 20 {
        io:println("Count is greater than 20");
    }
}

public function deleteNodeWithImport3(int count) {
    if count > 20 {
        io:println("Count is greater than 20");
    }
    if count <= 20 {
        io:println("Count is less or equal than 20");
    }
}
