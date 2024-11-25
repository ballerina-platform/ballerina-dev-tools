import ballerinax/kafka;
import ballerinax/rabbitmq;

service "newQueue" on new rabbitmq:Listener(host = "rabbitmq:DEFAULT_HOST", port = rabbitmq:DEFAULT_PORT) {
    remote function onRequest(rabbitmq:AnydataMessage message) returns anydata {
        do {
        } on fail error err {
            // handle error
        }
    }
}

service on new kafka:Listener(bootstrapServers = kafka:DEFAULT_URL, config = {groupId: "order-group-id", topics: ["order-topic"]}) {
    remote function onConsumerRecord(kafka:AnydataConsumerRecord[] records) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }

    remote function onError(kafka:Error kafkaError) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }
}
