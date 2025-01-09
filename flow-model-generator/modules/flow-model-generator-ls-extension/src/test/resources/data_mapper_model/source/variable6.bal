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
            UserInfo userInfo = {username: "un", password: "pw"};

		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
