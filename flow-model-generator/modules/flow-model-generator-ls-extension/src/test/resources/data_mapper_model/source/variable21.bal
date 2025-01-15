import ballerina/http;

type User1 record {|
    User2 user2;
    string s1;
|};

type User2 record {|
    User3 user3;
|};

type User3 record {|
    User4 user4;
    int i3;
|};

type User4 record {|
    User5 user5;
|};

type User5 record {|
    string[] u;
|};

type User6 record {|
    string account;
    decimal balance;
|};

type User record {|
    string name;
|};

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload User user) returns Person|http:InternalServerError {
        do {
            // User1 user1 = {user2: {user3: {i3: user.length(), user4: {user5: {u: [user.name, user.name]},users6: [{balance: 0.0, account: user.name}, {balance: 1.0, account: user.name}]}}}, s1: user.name}
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}