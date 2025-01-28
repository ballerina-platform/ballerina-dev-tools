import ballerina/time;

# User record type
# Simple record type with docs, anonymous field types (union, record)
type User record {
    # Name of the employee
    string name;
    # Age of the employee
    int age;
    # Union of of type-refs and a built-in type
    xml|City|UserAddress address;
    # Anonymous nested record type
    record {|int iA; record {|int iiA;|} iB;|} field1;
};

type TimeSheet time:Utc[][];

type City record {|
    string city;
    string country;
|};

# Record type with a rest field
type UserAddress record {|
    *City;
    # Union of built-in types
    string|int no;
    string street;
    string...;
|};

# Record type with a anonymous rest field
type ApartmentAddress record {|
    *City;
    string apartmentName;
    record {|int houseNo; string floor;|}...;
|};

# Record type with default values
type FooApartmentAddress record {|
    *City;
    string apartmentName = "Foo";
    City cityCountry = {country: "New York", city: "USA"};
    record {|int houseNo = 10; string floor = "1st";|}...;
|};

# Record type with array field type
type UserAsArrays record {
    string[] names;
    int[] ages;
    City[] addresses;
};
