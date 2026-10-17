package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.WorkEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с историей созданных документов.
 */
@Dao
interface WorkDao {

    @Query("SELECT * FROM works_history ORDER BY createdAt DESC")
    fun getAllWorks(): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works_history WHERE typeId = :typeId ORDER BY createdAt DESC")
    fun getWorksByType(typeId: String): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works_history WHERE id = :id LIMIT 1")
    suspend fun getWorkById(id: Long): WorkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(work: WorkEntity): Long

    @Update
    suspend fun update(work: WorkEntity)

    @Query("UPDATE works_history SET docxFilePath = :path WHERE id = :id")
    suspend fun updateDocxPath(id: Long, path: String)

    @Query("DELETE FROM works_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
