import ballerina/http;

type Person record {
    string fullName;
    record {
        string email;
        string primaryPhone;
    } contactDetails;
    record {
        string city;
        string state;
        string zipCode;
    } location;
    record {
        string accountNumber;
        int balance;
    } accountInfo;
    string transactionDate;
};

type User record {
    record {
        string firstName;
        string lastName;
        string email;
        record {
            string street;
            string city;
            string state;
            int postalCode;
        } address;
        string phoneNumber;
    } customer;
    record {
        string accountNumber;
        int balance;
        string lastTransaction;
    } account;
};

service / on new http:Listener(9090) {

    function init() returns error? {
    }

    resource function post getPerson(@http:Payload Person param) returns Person|http:InternalServerError {
        do {
            User user = check getUser("");
        } on fail error e {
            return http:INTERNAL_SERVER_ERROR;
        }
    }
}