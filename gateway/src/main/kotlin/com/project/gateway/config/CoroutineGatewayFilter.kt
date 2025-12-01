package com.project.gateway.config

import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class CoroutineGatewayFilter : GatewayFilter {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return mono {
            // 코루틴 블록에서 비동기 작업 수행
            val request = exchange.request
            println("Request Path: ${request.path}")
            chain.filter(exchange).awaitSingleOrNull()
        }
    }
}
