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
    User6[] users6;
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
            // User4[] users4 = [{users6: [{balance: 2.0, account: user.name}], user5: {u: [user.name, user.name]}}];
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}