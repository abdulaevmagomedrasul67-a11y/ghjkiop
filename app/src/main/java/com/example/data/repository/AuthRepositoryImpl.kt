package com.example.data.repository

import com.example.data.local.security.SessionManager
import com.example.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Long> {
        if (email.isBlank() || password.length < 4) {
            return Result.failure(Exception("Неверный email или пароль (минимум 4 символа)"))
        }
        val userId = sessionManager.getUserId()
        sessionManager.saveUserEmail(email)
        sessionManager.saveAuthToken("token_${System.currentTimeMillis()}")
        return Result.success(userId)
    }

    override suspend fun register(name: String, email: String, password: String): Result<Long> {
        if (email.isBlank() || password.length < 4 || name.isBlank()) {
            return Result.failure(Exception("Заполните все обязательные поля"))
        }
        val newUserId = System.currentTimeMillis() % 100000L
        sessionManager.saveUserId(newUserId)
        sessionManager.saveUserName(name)
        sessionManager.saveUserEmail(email)
        sessionManager.saveAuthToken("token_${System.currentTimeMillis()}")
        sessionManager.saveCoinBalance(500) // Приветственный бонус 500 SC
        return Result.success(newUserId)
    }

    override suspend fun logout() {
        sessionManager.logout()
    }

    override fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
}
