import ballerinax/kafka;

listener kafka:Listener kafkaListener = new (bootstrapServers = "localhost:9092", groupId = "unique-group-id", topics = "my-topic");

service on kafkaListener {
    remote function onConsumerRecord(kafka:AnydataConsumerRecord[] records) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }
}
