import ballerina/http;
import ballerinax/kafka;
import ballerinax/rabbitmq;

listener http:Listener httpListener = new (port = 9090);
listener http:Listener githubListener = new (port = 9091);

listener kafka:Listener kafkaListener = new (bootstrapServers = ["server1", "server2"], topics = ["topic1", "topic2"], groupId = "group-id");

listener rabbitmq:Listener rabbitmqListener = new (host = "host", port = 8090);
