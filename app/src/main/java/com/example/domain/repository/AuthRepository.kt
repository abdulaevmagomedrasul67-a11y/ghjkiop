package com.example.domain.repository

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Long>
    suspend fun register(name: String, email: String, password: String): Result<Long>
    suspend fun logout()
    fun isLoggedIn(): Boolean
}
