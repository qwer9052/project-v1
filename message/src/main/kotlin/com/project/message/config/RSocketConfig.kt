package com.project.message.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.messaging.rsocket.RSocketStrategies


@Configuration
class RSocketConfig {

    @Bean
    fun rsocketStrategies(): RSocketStrategies =
        RSocketStrategies.builder()
            .encoders { it.add(Jackson2JsonEncoder()) }
            .decoders { it.add(Jackson2JsonDecoder()) }
            .build()

}