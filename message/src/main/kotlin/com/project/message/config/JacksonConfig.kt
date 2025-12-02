package com.project.message.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class JacksonConfig {

    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer = Jackson2ObjectMapperBuilderCustomizer { builder ->
        builder.modulesToInstall(JavaTimeModule()) // 날짜/시간 타입 지원
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        builder.featuresToEnable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
        builder.postConfigurer { om ->
            om.registerKotlinModule() // ✅ 최신 권장: KotlinModule 대신 registerKotlinModule
        }
    }

}