package com.project.message.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableWebFluxSecurity
class ResourceServerSecurityConfig {

    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { } // 필요 시
            .authorizeExchange {
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers("/public/**").permitAll()
                it.pathMatchers("/rsocket").permitAll()
                it.pathMatchers("/admin/**").hasRole("admin") // realm 롤을 스프링 롤로 매핑
                it.anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { } // issuer-uri로 자동 설정됨
            }
            .build()
    }

}
