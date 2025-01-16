import ballerinax/kafka;

listener kafka:Listener kafkaListener = new kafka:Listener(bootstrapServers = "localhost:9092", groupId = "order-group-id", topics = ["order-topic"])

service on kafkaListener {
    remote function onConsumerRecord(kafka:AnydataConsumerRecord[] records) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }
}
