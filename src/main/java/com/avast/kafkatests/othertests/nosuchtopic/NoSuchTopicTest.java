package com.avast.kafkatests.othertests.nosuchtopic;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Test of sending to a topic that does not exist while automatic creation of topics is disabled in Kafka (auto.create.topics.enable=false).
 */
public class NoSuchTopicTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoSuchTopicTest.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.setProperty(ProducerConfig.CLIENT_ID_CONFIG, NoSuchTopicTest.class.getSimpleName());
        properties.setProperty(ProducerConfig.MAX_BLOCK_MS_CONFIG, "1000"); // Default is 60 seconds

        try (Producer<String, String> producer = new KafkaProducer<>(properties, new StringSerializer(), new StringSerializer())) {
            LOGGER.info("Sending message");
            producer.send(new ProducerRecord<>("ThisTopicDoesNotExist", "key", "value"), (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.error("Send failed: {}", exception.toString());
                } else {
                    LOGGER.info("Send successful: {}-{}/{}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

            LOGGER.info("Sending message");
            producer.send(new ProducerRecord<>("ThisTopicDoesNotExistToo", "key", "value"), (metadata, exception) -> {
                if (exception != null) {
                    LOGGER.error("Send failed: {}", exception.toString());
                } else {
                    LOGGER.info("Send successful: {}-{}/{}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        }
    }
}
