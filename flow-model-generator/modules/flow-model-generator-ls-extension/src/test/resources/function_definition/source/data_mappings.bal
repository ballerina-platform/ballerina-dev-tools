// Basic record definitions
type Person record {
    string name;
    int age;
};

type Employee record {
    string fullName;
    int yearsOld;
    decimal salary;
};

type Student record {
    string firstName;
    string lastName;
    int grade;
};

type Summary record {|
    decimal? averageAge;
    decimal totalSalary;
|};

// Simple function with direct mapping
function mapPersonToEmployee(Person person) returns Employee => {
    fullName: person.name,
    yearsOld: person.age,
    salary: 0.0
};

// Function with multiple parameters and mapping
function createEmployee(string name, int age, decimal salary) returns Employee => {
    fullName: name,
    yearsOld: age,
    salary
};

//  Using array mapping
function mapNames(Employee[] employees) returns Summary => 
    from var {yearsOld, salary} in employees
    collect {
        averageAge: avg(yearsOld),
        totalSalary: sum(salary)
    };
