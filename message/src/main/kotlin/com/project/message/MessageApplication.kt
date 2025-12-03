package com.project.message

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka

@SpringBootApplication // 👈 추가
@EnableKafka
class MessageApplication

fun main(args: Array<String>) {
    runApplication<MessageApplication>(*args)
}
