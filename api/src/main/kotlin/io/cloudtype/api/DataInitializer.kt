package io.cloudtype.api

import io.cloudtype.core.model.User
import io.cloudtype.core.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class DataInitializer {

    @Bean
    fun init(userRepository: UserRepository) = CommandLineRunner {
        val user = User(name = "Admin", email = "admin@example.com")
        userRepository.save(user)
    }
}