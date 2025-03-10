import ballerina/http;
import ballerina/time;

type Location record {|
    string city;
    string country;
|};

type Address record {|
    string houseNo;
    string line1;
    string line2;
    string city;
    string country;
|};

type Employee record {|
    string name;
    string empId;
    string email;
    Location location;
|};

type Person record {|
    string name;
    string email;
    Address address;
|};

type Admission record {
    string empId;
    string admissionDate;
};

type Event record {|
    string name;
    string description;
    time:Civil startTime;
    time:Civil endTime;
    Location location;
|};

type MyAcceptRec record {|
    *http:Accepted;
    time:Utc lastActive;
    string msg;
|};
