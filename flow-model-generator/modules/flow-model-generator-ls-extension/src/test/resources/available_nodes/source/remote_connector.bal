import ballerina/http;
import ballerinax/covid19;

final covid19:Client covidClient = check new ({}, "");

service /covid19 on new http:Listener(8080) {

    resource function get stats(http:Caller caller, http:Request req) returns error? {
    }

    resource function get countryStats(http:Caller caller, http:Request req, string country) returns error? {
    }
}
