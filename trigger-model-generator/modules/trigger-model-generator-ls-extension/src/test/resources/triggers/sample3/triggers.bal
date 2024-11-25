import ballerinax/kafka;

service on new kafka:Listener(bootstrapServers = kafka:DEFAULT_URL, config = {groupId: "order-group-id", topics: ["order-topic"]}) {
    remote function onConsumerRecord(kafka:AnydataConsumerRecord[] records) returns error? {
        do {
        } on fail error err {
            // handle error
        }
    }
}
