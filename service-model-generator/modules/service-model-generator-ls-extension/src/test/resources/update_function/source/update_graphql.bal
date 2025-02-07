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

    // GraphQL `Query`
    resource function get greeting(string name) returns string {
            return string `Hello, ${name}`;
    }

    // GraphQL `Mutation`
    remote function updateName(int id, string name) returns Profile|error {
        if profiles.hasKey(id) {
            Profile profile = profiles.remove(id);
            Profile updatedProfile = {
                id: profile.id,
                name: name,
                age: profile.age
            };
            profiles.put(updatedProfile);
            return updatedProfile;
        }
        return error(string `Profile with ID "${id}" not found`);
    }

    // GraphQL `Subscription`
    resource function subscribe names() returns stream<string, error?> {
        // Create a `NameGenerator` object.
        NameGenerator nameGenerator = new (self.names);
        // Create a stream using the `NameGenerator` object.
        stream<string, error?> names = new (nameGenerator);
        return names;
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
