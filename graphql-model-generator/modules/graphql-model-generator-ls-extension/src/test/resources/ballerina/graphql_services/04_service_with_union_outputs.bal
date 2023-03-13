import ballerina/graphql;


type SearchResult PersonalProfile|OfficeAddress;

distinct service class PersonalProfile {
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
}


distinct service class OfficeAddress {
    private final int number;
    private final string street;
    private final string city;

    function init(int number, string street, string city) {
        self.number = number;
        self.street = street;
        self.city = city;
    }

    resource function get number() returns int {
        return self.number;
    }

    resource function get street() returns string {
        return self.street;
    }

    resource function get city() returns string {
        return self.city;
    }
}

service /graphql on new graphql:Listener(9090) {

    resource function get search(string keyword) returns SearchResult[] {
        return [new ("Walter White", 50), new (308, "Negra Arroyo Lane", "Albuquerque")];
    }

    remote function searchDetails(string keyword) returns PersonalProfile|OfficeAddress{
        return [new ("Walter White", 50), new (308, "Negra Arroyo Lane", "Albuquerque")];
    }
}
