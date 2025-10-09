package com.project.api.service


import com.project.core.model.User
import com.project.core.repository.UserRepository;
import org.springframework.stereotype.Service


@Service("UserService2")
class UserService(private val userRepository:UserRepository) {

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun getUserById(id: Long): User = userRepository.findById(id).orElseThrow { Exception("User not found") }

    fun createUser(user: User): User = userRepository.save(user)

    fun updateUser(id: Long, userDetails: User): User {
        val user = getUserById(id)
        val updatedUser = user.copy(name = userDetails.name, email = userDetails.email)
        return userRepository.save(updatedUser)
    }

    fun deleteUser(id: Long) {
        val user = getUserById(id)
        userRepository.delete(user)
    }
}
