import ballerina/graphql;
import ballerina/http;
import ballerina/io;
import ballerina/tcp;

// HTTP connection
final http:Client httpConnection = check new ("https://api.example.com", {
    timeout: 10,
    retryConfig: {
        count: 3,
        interval: 3,
        backOffFactor: 2.0
    }
});

// GraphQL connection
final graphql:Client graphQlConnection = check new ("https://graphql.example.com/graphql", {
    timeout: 30
});

// TCP connection
final tcp:Client tcpConnection = check new ("localhost", 9090, {
    writeTimeout: 10
});

// Create a variable from the local client class
final LocalClient localClient = check new ("http://localhost:8080", 20);

// Define a local client class
public client class LocalClient {
    private string baseUrl;
    private int timeout;

    public isolated function init(string baseUrl, int timeout = 30) returns error? {
        self.baseUrl = baseUrl;
        self.timeout = timeout;
        io:println("LocalClient initialized with baseUrl: " + baseUrl);
    }

    remote isolated function getData(string id) returns string|error {
        io:println("Fetching data for id: " + id);
        // Actual implementation would go here
        return "Data for " + id;
    }
}
