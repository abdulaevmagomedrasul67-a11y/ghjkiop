package com.example.domain.repository

import com.example.domain.model.GostProfile
import com.example.domain.model.UniversityProfile
import kotlinx.coroutines.flow.Flow

interface UniversityProfileRepository {
    fun getSelectedProfile(): Flow<UniversityProfile>
    suspend fun selectProfile(profile: UniversityProfile)
    suspend fun getAllProfiles(): List<UniversityProfile>

    fun getAllGostProfiles(): Flow<List<GostProfile>>
    suspend fun getProfileById(id: Long): GostProfile?
    suspend fun getDefaultProfile(): GostProfile
    suspend fun insertProfile(profile: GostProfile): Long
    suspend fun updateProfile(profile: GostProfile) {
        insertProfile(profile)
    }
    suspend fun deleteProfile(id: Long)
}
