import ballerina/http;

listener http:Listener httpListener = new (port = 9090);

service /api on httpListener {

}
