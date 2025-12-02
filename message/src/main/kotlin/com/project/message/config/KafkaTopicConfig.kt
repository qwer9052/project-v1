package com.project.message.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class KafkaTopicConfig {
    @Bean
    fun chatTopic(): NewTopic {
        // 로컬 단일 브로커: replicationFactor=1
        // 멀티 브로커(3노드): replicationFactor=3 권장
        return NewTopic("chat-messages", 6, 1)
    }

}