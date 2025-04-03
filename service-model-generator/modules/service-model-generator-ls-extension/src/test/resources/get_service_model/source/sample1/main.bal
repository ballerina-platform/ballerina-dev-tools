import ballerinax/kafka;

listener kafka:Listener kafkaListener = new (bootstrapServers = "localhost");
