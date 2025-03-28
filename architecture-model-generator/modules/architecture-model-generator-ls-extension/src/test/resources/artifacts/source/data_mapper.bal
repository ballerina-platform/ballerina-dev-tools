import ballerina/io;

// Define some sample record types
type Person record {
    string name;
    int age;
    string address;
};

type Employee record {
    string firstName;
    string lastName;
    int age;
    string department;
    float salary;
};

type Customer record {
    string id;
    string fullName;
    string email;
    string[] contactNumbers;
};

type CustomerSummary record {
    string id;
    string name;
    string primaryContact;
};

// Simple primitive type mappers
function celsiusToFahrenheit(float celsius) returns float => celsius * 9/5 + 32;

function fahrenheitToCelsius(float fahrenheit) returns float => (fahrenheit - 32) * 5/9;

function stringToInt(string value) returns int|error => int:fromString(value);

function concatenateStrings(string first, string last) returns string => string `${first} ${last}`;

// Record mappers
function personToEmployee(Person person) returns Employee => {
    firstName: person.name,
    lastName: person.name,
    age: person.age,
    department: "Not Assigned",
    salary: 0.0
};

function customerToCustomerSummary(Customer customer) returns CustomerSummary => {
    id: customer.id,
    name: customer.fullName,
    primaryContact: customer.contactNumbers.length() > 0 ? customer.contactNumbers[0] : ""
};

function createPerson(string name, int age) returns Person => {
    name: name,
    age: age,
    address: ""
};

// Array transformation mapper
function doubleAllValues(int[] numbers) returns int[] => 
    from int num in numbers
    select num * 2;

function extractNames(Person[] people) returns string[] =>
    from var person in people
    select person.name;

public function main() {
    // Test primitive type mappers
    float tempC = 30.0;
    io:println("30°C in Fahrenheit: " + celsiusToFahrenheit(tempC).toString());
    
    string num = "42";
    var intValue = stringToInt(num);
    io:println("Converted string to int: ", intValue);
    
    // Test record mappers
    Person person = {
        name: "John Doe",
        age: 30,
        address: "123 Main St"
    };
    
    Employee employee = personToEmployee(person);
    io:println("Converted Person to Employee: ", employee);
    
    Customer customer = {
        id: "CUST001",
        fullName: "Jane Smith",
        email: "jane@example.com",
        contactNumbers: ["+1234567890", "+1987654321"]
    };
    
    CustomerSummary summary = customerToCustomerSummary(customer);
    io:println("Customer Summary: ", summary);
    
    // Test array mapper
    int[] numbers = [1, 2, 3, 4, 5];
    int[] doubled = doubleAllValues(numbers);
    io:println("Doubled numbers: ", doubled);
}
