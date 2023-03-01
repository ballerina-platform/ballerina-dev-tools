import ballerina/graphql;

service class Profile {
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


# Address description
#
# + number - number
# + street - street of Address
# + city - provided city
public type Address record {|
    string number;
    Street street;
    string city;
|};

public type Street record {|
    string mainStreet;
    string subStreet;
|};

# Team enum
enum Team {
    ENGINEERING,
    MARKETING,
    # # Deprecated
    # The `SALES` is deprecated.
    @deprecated
    SALES
}

# Sample GraphQL service
service /graphql on new graphql:Listener(9090) {

    private string[] names;

    function init() {
        self.names = ["Walter White", "Jesse Pinkman", "Skyler White"];
    }


    # The resource maps to a output object type Profile
    # + return - The Profile object
    resource function get profile() returns Profile {
        return new ("Walter White", 51);
    }

    # # Deprecated
    # The `address` field is deprecated.
    # + return - The Address object
    @deprecated
    resource function get address() returns Address {
        return {number: "123", street: {mainStreet: "Main Street", subStreet: "Sub Street"}, city: "Colombo"};
    }

    # The remote functions outputs Team
    # + teamId - The team ID
    # + return - The Team object
    remote function getTeam(int teamId) returns Team {
        if (teamId == 1) {
            return ENGINEERING;
        } else if (teamId == 2) {
            return MARKETING;
        } else {
            return SALES;
        }
    }

    resource function subscribe names() returns stream<string> {
        return self.names.toStream();
    }
}
