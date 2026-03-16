package com.plgdhd.authservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topics.user-registered}")
    private String userRegisteredTopic;

    @Value("${app.kafka.topics.user-banned}")
    private String userBannedTopic;

    @Value("${app.kafka.topics.user-role-changed}")
    private String userRoleChangedTopic;


    // передача через protobuf
    @Bean
    public ProducerFactory<String, byte[]> producerFactory() {

        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // для userId (UUID)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // для protobuf byte[]
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);

        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 32768);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, byte[]> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }


    //TODO Заменить эту поебень через terraform
    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder.name(userRegisteredTopic)
                .partitions(3)
                .replicas(1)   // В проде: 3
                .config("retention.ms", "604800000") // 7 дней
                .config("cleanup.policy", "delete")
                .config("min.insync.replicas", "1") // В проде: 2
                .build();
    }

    @Bean
    public NewTopic userBannedTopic() {
        return TopicBuilder.name(userBannedTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "2592000000")  // 30 дней
                .config("min.insync.replicas", "1")
                .build();
    }

    @Bean
    public NewTopic userRoleChangedTopic() {
        return TopicBuilder.name(userRoleChangedTopic)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

}
