import ballerinax/kafka;

listener kafka:Listener kafkaListener = new (bootstrapServers = "localhost");

service kafka:Service on kafkaListener {
    remote function onConsumerRecord(kafka:AnydataConsumerRecord[] records, kafka:Caller caller) returns error? {
        do {
        } on fail error err {
            // handle error
            return error("Not implemented", err);
        }
    }

}