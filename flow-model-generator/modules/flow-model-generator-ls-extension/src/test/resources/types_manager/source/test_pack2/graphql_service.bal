import ballerina/graphql;

public type AbstractProfile record {|
    string name;
    int age;
    Address address;
|};

service class UserProfile {
    private final string name;
    private final int age;

    function init(string name, int age) {
        self.name = name;
        self.age = age;
    }

    resource function get name() returns string {
        return self.name;
    }
    resource function get age() returns int {
        return self.age;
    }
    resource function get isAdult() returns boolean {
        return self.age > 21;
    }
}

public type Address record {|
    string number;
    string street;
    string city;
|};

service /graphql on new graphql:Listener(9090) {
    resource function get profile21() returns AbstractProfile {
        return {
            name: "Walter White",
            age: 51,
            address: {
                number: "308",
                street: "Negra Arroyo Lane",
                city: "Albuquerque"
            }
        };
    }

    resource function get profile() returns UserProfile {
        return new ("Walter White", 51);
    }
}
