import ballerina/http;

type Studentsss record {|
    string[][][] names;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            // Studentsss var1 = {names: [[["1", "2"]], [["3"]]]};
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
