package com.plgdhd.authservice.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventSender implements EventSender {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public KafkaEventSender(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, String key, byte[] data) {
        kafkaTemplate.send(topic, key, data)
                .whenComplete((result,ex) -> {
                    if (ex == null) {
                        log.info("Event sent to topic {}. Key: {}", topic, key);
                    } else {
                        log.error("Failed to send event to Kafka topic {}: {}", topic, ex.getMessage(), ex);
                    }

                });
    }
}
