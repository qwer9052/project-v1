package com.project.message.config

import com.project.message.dto.ChatMessageDTO
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer


@Configuration
class KafkaTopicConfig {

    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String

    // ========== Topic 설정 ==========
    @Bean
    fun chatMessagesTopic(): NewTopic {
        return TopicBuilder.name("chat-messages")
            .partitions(3)
            .replicas(1)
            // 메시지 보관 기간: 7일 (604800000ms)
            .config("retention.ms", "604800000")
            // 메시지 크기 제한: 10MB
            .config("segment.bytes", "10485760")
            // 정책: 크기 또는 시간 (시간이 먼저 도달하면 삭제)
            .config("retention.bytes", "-1")
            .build()
    }

    // ========== Producer (발행자) 설정 ==========
    @Bean
    fun producerFactory(): ProducerFactory<String, ChatMessageDTO> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 10
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, ChatMessageDTO> {
        return KafkaTemplate(producerFactory())
    }

    // ========== Consumer (소비자) 설정 ==========
    @Bean
    fun consumerFactory(): ConsumerFactory<String, ChatMessageDTO> {
        val configProps = mapOf(
            org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG to "chat-consumer",
            org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to JsonDeserializer::class.java,
            JsonDeserializer.VALUE_DEFAULT_TYPE to ChatMessageDTO::class.java.name,
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "latest",
            org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 100
        )
        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun kafkaListenerContainerFactory(): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ChatMessageDTO>> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ChatMessageDTO>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(3)
        return factory
    }

    // ========== Admin (관리) 설정 ==========
    @Bean
    fun kafkaAdmin(): org.springframework.kafka.core.KafkaAdmin {
        val adminConfigs = mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
        )
        return org.springframework.kafka.core.KafkaAdmin(adminConfigs)
    }
}