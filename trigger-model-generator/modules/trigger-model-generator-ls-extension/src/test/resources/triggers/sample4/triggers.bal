import ballerinax/kafka;
import ballerinax/rabbitmq;

@display {
    label: "rabbitmq-svc"
}
service "OrderQueue" on new rabbitmq:Listener(host = "localhost", port = 9090, username = "admin", password = "admin") {
    remote function onRequest(rabbitmq:AnydataMessage message) returns anydata {
        do {
        } on fail error err {
            // handle error
        }
    }
}

@display {
    label: "kafka-service"
}
service on new kafka:Listener(bootstrapServers = "localhost:9092", groupId = "order-group-id", topics = ["order-topic"]}) {
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
