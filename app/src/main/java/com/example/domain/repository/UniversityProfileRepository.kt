package com.example.domain.repository

import com.example.domain.model.UniversityProfile
import kotlinx.coroutines.flow.Flow

interface UniversityProfileRepository {
    fun getProfiles(): List<UniversityProfile>
    fun getSelectedProfile(): Flow<UniversityProfile>
    suspend fun selectProfile(id: String)
    suspend fun addCustomProfile(profile: UniversityProfile)
}
