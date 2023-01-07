import ballerina/graphql;

// Service attached to a GraphQL listener exposes a GraphQL service on the provided port.
service /graphql2 on new graphql:Listener(4001) {

     // Define a `string` array in the service.
    private string[] names;

    function init() {
        // Initialize the array.
        self.names = ["Walter White", "Jesse Pinkman", "Skyler White"];
    }

    // Returning the `Animal` type from a GraphQL resolver will idenitify it as an interface
    resource function get animals() returns Elephant[] {
        return [new Elephant()];
    }

    resource function get animalType(string typeName = "test") returns Panda|Elephant {
        return new Elephant();
    }

    remote function  updateAnimal(int newage) returns Lion {
        return {name: "", age: 0};
    }
}

// Define the interface `Animal` using a `distinct` `service` object
public type Animal distinct service object {

    // Define the field `name` as a resource function definition
    resource function get name() returns string;
};

// Another class implementing the `Mammal` class
public distinct service class Elephant {
    *Animal;

    resource function get name() returns string {
        return "Elephas maximus maximus";
    }

    resource function get call() returns string {
        return "Trumpet";
    }
}

public distinct service class Panda {
    *Animal;

    resource function get name() returns string {
        return "Elephas maximus maximus";
    }

    resource function get call() returns string {
        return "Trumpet";
    }
}

type Lion record {|
    string name;
    int age;
|};