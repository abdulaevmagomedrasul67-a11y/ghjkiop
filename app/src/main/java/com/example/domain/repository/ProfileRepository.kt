package com.example.domain.repository

import com.example.data.remote.dto.UserProfileDto
import com.example.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val currentUserFlow: StateFlow<User>
    fun getCurrentUser(): User
    suspend fun refreshUser(): User
    suspend fun getProfile(): Result<UserProfileDto>
    suspend fun updateProfile(name: String, email: String, university: String? = null): Result<Unit>
    suspend fun registerPushToken(token: String): Result<Unit>
    suspend fun logout()
}
