package com.plgdhd.authservice.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaEventSender implements EventSender {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    @Autowired
    public KafkaEventSender(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, String key, byte[] data) {
        kafkaTemplate.send(topic, key, data)
                .whenComplete((result,ex) -> {
                    if(ex == null) {
                        log.info("Регистрация пользователя успешно отправлена в топик {}. Ключ: {}",
                                topic, key);
                    }
                    else{
                        log.error("Ошибка отправки регистрации пользователя в Kafka (топик {}): {}", topic, ex.getMessage(), ex);
                        // TODO сделать логику обработки этой ситуации, сейчас 3 ночи и я заебался (X﹏X)
                    }

                });
    }
}
