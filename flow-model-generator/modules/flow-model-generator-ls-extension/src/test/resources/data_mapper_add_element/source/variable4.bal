import ballerina/http;

type Credentials record {|
    string username;
    string password;
    string[] ids;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            // Credentials[] credentials = [{username: "uname", password: "pword", ids: ["id1", "id2"]}];
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
