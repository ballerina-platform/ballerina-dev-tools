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

type Bank record {|
    string name;
    UserInfo[] userInfo;
|};

type Store record {|
    string name;
    Credentials[] credentials;
|};

const string CONST = "CONST";

service OASServiceType on new http:Listener(9090) {

	resource function get pet() returns int|http:NotFound {
        do {
            UserInfo[] userInfo = [{username: "un", password: "pw", id: 3}, {username: "un1", password: "pw1", id: 5}];
            Bank[] banks = [{name: "Alis", userInfo: userInfo}];
            // Store[] stores = from var bank in banks
            //     select {
            //         name: bank.name,
            //         credentials: from var userInfo in bank.userInfo
            //             select {
            //                password: ,
            //                username: userInfo.username
            //            }
            //    };
		} on fail error e {
			return http:NOT_FOUND;
		}
	}
}
