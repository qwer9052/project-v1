package com.project.gateway.config

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class GlobalGatewayFilter {

    private val log = LoggerFactory.getLogger("GatewayRequestLogger")

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // pre 단계에서 가장 먼저
    fun requestLoggingFilter(): GlobalFilter = GlobalFilter { exchange: ServerWebExchange, chain: GatewayFilterChain ->
        val req = exchange.request
        log.info("REQ {} {} from={} headers={}", req.method, req.uri, req.remoteAddress, req.headers)
        chain.filter(exchange).then(
            Mono.fromRunnable {
                val res = exchange.response
                log.info("RES {} {} status={} headers={}", req.method, req.uri, res.statusCode, res.headers)
            }
        )
    }


}
