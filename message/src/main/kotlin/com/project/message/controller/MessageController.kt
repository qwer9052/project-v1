package com.project.message.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
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

    @GetMapping("/test2")
    fun test2(): String {
        return "test2"
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: org.springframework.security.oauth2.jwt.Jwt): Map<String, Any?> {
        val realmRoles = (jwt.claims["realm_access"] as? Map<*, *>)?.get("roles")
        val resourceAccess = jwt.claims["resource_access"] as? Map<*, *>
        val gatewayRoles = (resourceAccess?.get("gateway") as? Map<*, *>)?.get("roles")

        return mapOf(
            "email" to jwt.claims["email"],
            "username" to jwt.claims["preferred_username"],
            "name" to jwt.claims["name"],
            "sub" to jwt.subject,
            "preferred_username" to jwt.claims["preferred_username"],
            "scope" to jwt.claims["scope"],
            "realm_roles" to realmRoles,
            "gateway_roles" to gatewayRoles
        )
    }

}