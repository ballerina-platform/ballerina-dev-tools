import ballerina/http;

type Credentials record {|
    string username;
    string password;
|};

type UserInfo record {|
    string username;
    string password;
    int id;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            UserInfo[] userInfo = [{username: "un", password: "pw", id: 3}, {username: "un1", password: "pw1", id: 5}];
            // Credentials[] credentials = userInfo;
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
