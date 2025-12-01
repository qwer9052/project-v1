package com.project.message.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class MessageController {
    @GetMapping("/message/{id}")
    fun getMessageById(@PathVariable id: String): String {
        return "asdasd"
    }
    @GetMapping("/test")
    fun test(): String {
        return "test"
    }
}