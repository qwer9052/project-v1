package com.project.api

import com.project.core.model.User
import com.project.core.repository.UserRepository
import com.project.domain.service.UserService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class DataInitializer(
    private val userService: UserService
) {

    @Bean
    fun init(userRepository: UserRepository) = CommandLineRunner {
        userService.configure()
        runBlocking {
            if (userRepository.findUserByName("Admin") == null) {
                val user = User(name = "Admin", email = "admin@example.com")
                userRepository.save(user)
            }
        }
    }
}