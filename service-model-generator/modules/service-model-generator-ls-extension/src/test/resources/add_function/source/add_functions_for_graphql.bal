import ballerina/graphql;

listener graphql:Listener graphQLListener = new (9090);

// Defines a `record` type to use as an object in the GraphQL service.
type Profile readonly & record {|
    int id;
    string name;
    int age;
|};

// Defines an in-memory table to store the profiles.
table<Profile> key(id) profiles = table [
        {id: 1, name: "Walter White", age: 50},
        {id: 2, name: "Jesse Pinkman", age: 25}
    ];

service /graphql on graphQLListener {

    private final readonly & string[] names = ["Walter White", "Jesse Pinkman", "Saul Goodman"];

    function init() returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

}

class NameGenerator {
    private final string[] names;

    isolated function init(string[] names) {
        self.names = names;
    }

    // The `next` method picks a random name from the list and returns it.
    public isolated function next() returns record {|string value;|}|error? {
        return {value: self.names[0]};
    }
}
