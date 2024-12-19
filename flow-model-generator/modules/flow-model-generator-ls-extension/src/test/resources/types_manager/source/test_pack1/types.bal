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

type MixRecord record {|
    time:Utc|Person|Address mixField1;
    Address|Person|int mixField2;
    map<Address> mixField3;
|};

enum Color {
    RED,
    YELLOW,
    GREEN
};

type Employees record {|
    int id;
    string name;
|}[];

type Names string[];

type User [Person, int];

type Misc Person|Employees|Color;

type PersonError error<Person>;
