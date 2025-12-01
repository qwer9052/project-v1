package com.project.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
class SecurityConfig {
    @Bean
    fun springSecurity(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() } // API만 사용하는 SPA면 보통 비활성(상황에 따라 조정)
            .authorizeExchange { auth ->
                auth.pathMatchers("/login**", "/actuator/**").permitAll()
                auth.anyExchange().authenticated()
            }
            .oauth2Login { it -> }
            .build()
}
