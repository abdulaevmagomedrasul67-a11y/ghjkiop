package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UniversityProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с профилями ВУЗов и ГОСТ.
 */
@Dao
interface UniversityProfileDao {

    @Query("SELECT * FROM university_profiles ORDER BY isDefault DESC, name ASC")
    fun getAllProfiles(): Flow<List<UniversityProfileEntity>>

    @Query("SELECT * FROM university_profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): UniversityProfileEntity?

    @Query("SELECT * FROM university_profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): UniversityProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: UniversityProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<UniversityProfileEntity>)

    @Update
    suspend fun update(profile: UniversityProfileEntity)

    @Query("DELETE FROM university_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE university_profiles SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE university_profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultFlag(id: Long)

    @Query("SELECT COUNT(*) FROM university_profiles")
    suspend fun countProfiles(): Int
}
