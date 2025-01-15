import ballerina/http;

type Address record {|
   string street;
   string city;
|};

type Student record {|
   string username;
   string password;
   Address[] address;
|};

type Department record {|
    string name;
    Student[] students;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            // Department department = {name: "DEPT1", students: [{username: "Alex", password: "xelA", address: [{street: "s1", city: "c1"}]}]};
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
