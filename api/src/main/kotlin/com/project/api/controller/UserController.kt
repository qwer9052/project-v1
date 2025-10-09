package com.project.api.controller

import com.project.core.model.User
import com.project.api.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @GetMapping
    suspend fun getAllUsers(): List<User> = userService.getAllUsers()

    @GetMapping("/{id}")
    suspend fun getUserById(@PathVariable id: Long): User = userService.getUserById(id)

    @PostMapping
    suspend fun createUser(@RequestBody user: User): User = userService.createUser(user)

    @PutMapping("/{id}")
    suspend fun updateUser(@PathVariable id: Long, @RequestBody user: User): User = userService.updateUser(id, user)

    @DeleteMapping("/{id}")
    suspend fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        userService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }
}