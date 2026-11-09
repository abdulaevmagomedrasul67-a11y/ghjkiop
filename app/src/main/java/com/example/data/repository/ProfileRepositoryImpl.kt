package com.example.data.repository

import com.example.data.local.security.SessionManager
import com.example.data.remote.dto.UserProfileDto
import com.example.domain.model.User
import com.example.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileRepositoryImpl(
    private val sessionManager: SessionManager
) : ProfileRepository {

    private val _currentUserFlow = MutableStateFlow(loadUserFromSession())
    override val currentUserFlow: StateFlow<User> = _currentUserFlow.asStateFlow()

    private fun loadUserFromSession(): User {
        return User(
            id = sessionManager.getUserId(),
            fullName = sessionManager.getUserName(),
            email = sessionManager.getUserEmail(),
            balanceSc = 0
        )
    }

    override fun getCurrentUser(): User = _currentUserFlow.value

    override suspend fun refreshUser(userId: Long): Result<User> {
        val user = loadUserFromSession()
        _currentUserFlow.value = user
        return Result.success(user)
    }

    override suspend fun getProfile(): Result<UserProfileDto> {
        val profile = UserProfileDto(
            id = sessionManager.getUserId(),
            name = sessionManager.getUserName(),
            email = sessionManager.getUserEmail(),
            university = "Московский государственный университет",
            faculty = "Факультет прикладной математики",
            coinBalance = 0
        )
        return Result.success(profile)
    }

    override suspend fun updateProfile(name: String, email: String, university: String?): Result<Unit> {
        sessionManager.saveUserName(name)
        sessionManager.saveUserEmail(email)
        _currentUserFlow.value = loadUserFromSession()
        return Result.success(Unit)
    }

    override suspend fun updateProfile(userId: Long, fullName: String, email: String): Result<Boolean> {
        sessionManager.saveUserName(fullName)
        sessionManager.saveUserEmail(email)
        _currentUserFlow.value = loadUserFromSession()
        return Result.success(true)
    }

    override suspend fun registerPushToken(token: String): Result<Unit> {
        sessionManager.savePushToken(token)
        return Result.success(Unit)
    }

    override suspend fun logout() {
        sessionManager.logout()
        _currentUserFlow.value = loadUserFromSession()
    }
}
