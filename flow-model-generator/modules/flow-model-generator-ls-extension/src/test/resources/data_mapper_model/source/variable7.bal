import ballerina/http;

type Credentials record {|
   string username;
   string password;
|};

type UserInfo record {|
   string username;
   string password;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            UserInfo[] userInfo = [{username: "un", password: "pw"}, {username: "un1", password: "pw1"}];
            // Credentials[] credentials = [{username: userInfo[0].username, password: userInfo[0].password}];
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
