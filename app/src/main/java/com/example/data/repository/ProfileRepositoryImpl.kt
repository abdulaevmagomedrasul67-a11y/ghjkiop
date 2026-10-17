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

    private val _currentUserFlow = MutableStateFlow(getCurrentUser())
    override val currentUserFlow: StateFlow<User> = _currentUserFlow.asStateFlow()

    override fun getCurrentUser(): User {
        return User(
            id = sessionManager.getUserId(),
            fullName = sessionManager.getUserName(),
            email = sessionManager.getUserEmail(),
            balanceSc = sessionManager.getCoinBalance()
        )
    }

    override suspend fun refreshUser(): User {
        val user = getCurrentUser()
        _currentUserFlow.value = user
        return user
    }

    override suspend fun getProfile(): Result<UserProfileDto> {
        val profile = UserProfileDto(
            id = sessionManager.getUserId(),
            name = sessionManager.getUserName(),
            email = sessionManager.getUserEmail(),
            university = "Московский государственный университет",
            faculty = "Факультет прикладной математики",
            coinBalance = sessionManager.getCoinBalance()
        )
        return Result.success(profile)
    }

    override suspend fun updateProfile(name: String, email: String, university: String?): Result<Unit> {
        sessionManager.saveUserName(name)
        sessionManager.saveUserEmail(email)
        _currentUserFlow.value = getCurrentUser()
        return Result.success(Unit)
    }

    override suspend fun registerPushToken(token: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun logout() {
        sessionManager.logout()
    }
}
