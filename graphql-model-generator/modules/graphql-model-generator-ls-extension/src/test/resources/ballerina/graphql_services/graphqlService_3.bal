import ballerina/graphql;

# Represents a Person
#
# + name - Name of the person
# + age - age of the person
# + address - Addres of the person
type Person record {
    string name = "Walter";
    int age;
    Address address;
};

# Represents an address.
#
# + number - The number of the address
# + street - The street of the address
# + city - The city of the address
type Address record {
    int number;
    string street;
    string city = "Albuquerque";
};

isolated service on new graphql:Listener(9000) {

    isolated resource function get city(Person person) returns string {
        return person.address.city;
    }
}
