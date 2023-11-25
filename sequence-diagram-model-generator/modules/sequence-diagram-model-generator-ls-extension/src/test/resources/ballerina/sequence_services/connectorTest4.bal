import ballerina/http;

private http:Client diseaseEndpoint = check new ("https://disease.sh/v3");
http:Client diseaseEndpoint3 = diseaseEndpoint;

public function foo() returns error? {
    http:Client diseaseEp2 = check new ("https://disease.sh/v3");
    Country[] countries = check diseaseEp2->/covid\-19/countries;
}

