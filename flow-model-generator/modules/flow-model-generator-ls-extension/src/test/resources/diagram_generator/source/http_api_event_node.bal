import ballerina/http;

type Menu record {|
    string modified;
    int year;
    string[] items;
|};

service /market on new http:Listener(9090) {
    resource function get menu() returns Menu => {year: 0, modified: "", items: []};
    resource function get menu/[int year]() returns Menu => {year, modified: "", items: []};

    resource function post menu/[int year]/[boolean... flags](string item) {
    }
    
}
