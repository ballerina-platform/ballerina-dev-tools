import ballerina/http;
import ballerinax/rabbitmq;

listener http:Listener refListener = new (9090);
listener rabbitmq:Listener rabbitmqListener = new (host = "localhost", port = 5672);
listener http:Listener httpDefaultListener = http:getDefaultListener();
listener httpListener = new http:Listener(9090);
