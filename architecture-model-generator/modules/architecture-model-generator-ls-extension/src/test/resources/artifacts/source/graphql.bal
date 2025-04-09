import ballerina/graphql;

// Define types for the GraphQL schema
public type Person record {|
    string id;
    string name;
    int age;
    string email;
|};

// Mock database for the example
Person[] people = [
    {id: "1", name: "John Doe", age: 30, email: "john@example.com"},
    {id: "2", name: "Jane Smith", age: 25, email: "jane@example.com"},
    {id: "3", name: "Bob Johnson", age: 40, email: "bob@example.com"}
];

// Define the service class
service class Query {
    resource function get people() returns Person[] {
        return people;
    }
    
    resource function get person(string id) returns Person? {
        foreach Person p in people {
            if p.id == id {
                return p;
            }
        }
        return ();
    }
}

// Define mutations
service class Mutation {
    remote function addPerson(string name, int age, string email) returns Person {
        string id = (people.length() + 1).toString();
        Person newPerson = {id: id, name: name, age: age, email: email};
        people.push(newPerson);
        return newPerson;
    }
}

// Create and start the GraphQL service
service /graphql on new graphql:Listener(9000) {
    resource function get greeting() returns string {
        return "Hello, GraphQL!";
    }
    
    // Include the query and mutation service classes
    Query query = new;
    Mutation mutation = new;
}
