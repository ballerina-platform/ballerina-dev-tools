import ballerina/time;

type Address record {|
    string houseNo;
    string line1;
    string line2;
    string city;
    string country;
|};

# Person record
# Used to represent person object
type Person record {|
    # id of the person
    int id = 12;
    # name of the person
    string name = "John";
    # addresses of the person
    Address[] addresses = [
        {
            houseNo: "10",
            line1: "5th Ave",
            line2: "4th St",
            city: "New York City",
            country: "USA"
        }
    ];
    # date of birth
    time:Utc dob = getDefaultDob();
|};

type MixRecord record {
    time:Utc|Person|Address mixField1;
    Address|Person|int mixField2;
    map<Address> mixField3;
};

type Student record {|
    *Person;
    int studentId = 22;
|};

enum Color {
    RED,
    YELLOW,
    GREEN
};

enum Gender {
    MALE = "male",
    FEMALE = "female"
}

type Employees record {|
    int id;
    string name;
    Names otherNames;
|}[];

type Names string[];

type PersonWithId [int, Person];

type Misc Person|Employees|Color;

type PersonError error<Person>;

type AddressTable table<Address> key<string|int>;

type PartTimeStudent record {|
    *Student;
    string:Char code;
|};

# Represents the type of the vehicle
public type VehicleType isolated client object {
    string make;
    string model;

    # Get the make of the vehicle
    isolated remote function getMake() returns string;
    public isolated function setYear(int year);
};

# Vehicle class.
public isolated readonly distinct client class Vehicle {
    *VehicleType;
    # Year of manufacturing.
    private int year;

    function init(string make, string model, int year) {
        self.make = make;
        self.model = make;
        self.year = year;
    }

    isolated resource function get [int id]/country() returns string {
        return "Japan";
    }

    private isolated function getYear(string time) returns int {
        return self.year;
    }

    public isolated function setYear(int year) {
        return;
    }

    isolated remote function getMake() returns string {
        return "";
    }
}
