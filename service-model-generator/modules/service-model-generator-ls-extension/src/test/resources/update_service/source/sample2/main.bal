import ballerina/http;

listener http:Listener httpListener = new (port = 9090);

@http:ServiceConfig {
    host: "localhost"
}
service /api/test on httpListener {

}
