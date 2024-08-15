import ballerina/http;

http:Client cl = check new ("http://localhost:9090");

function logEmployee(Employee employee) {

}

public function main() {
    // Define local variables for input parameters
    string name = "John Doe";
    string email = "john.doe@example.com";
    Address address = {
        houseNo: "123",
        line1: "Main Street",
        line2: "Apartment 4B",
        city: "New York",
        country: "USA"
    };

    // Transform to Person
    Person person = transformToPerson(name, email, address);

    Admission admission = {
        empId: "EMP001",
        admissionDate: "2024-08-15"
    };

    // Transform to Employee
    Employee employee = transformToEmployee(person, admission);
    logEmployee(employee);
}
